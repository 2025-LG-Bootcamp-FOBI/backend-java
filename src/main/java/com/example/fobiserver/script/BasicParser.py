import fitz  # PyMuPDF
import sys
import json
import os
import uuid
from collections import defaultdict, Counter

def guess_pdf_headings(doc):
    font_texts = []  # (text, font_size, page_number)
    font_counter = Counter()

    # 1. 폰트 사이즈별로 텍스트를 수집하고 카운트
    for i in range(len(doc)):
        blocks = doc.load_page(i).get_text("dict")["blocks"]
        for block in blocks:
            for line in block.get("lines", []):
                spans = line.get("spans", [])
                if not spans:
                    continue
                text = " ".join([span["text"] for span in spans]).strip()
                font_size = spans[0]["size"]
                if font_size > 0 and 5 < len(text) < 100:  # 의미 있는 텍스트만 선택
                    font_texts.append((text, font_size, i + 1))
                    font_counter[font_size] += 1

    if not font_counter:
        return []

    # 2. 가장 많이 등장한 폰트 사이즈 찾기 (본문 텍스트로 가정)
    most_common_font, _ = font_counter.most_common(1)[0]

    # 3. 본문보다 큰 폰트만 사용하여 레벨 매핑
    filtered_fonts = sorted([f for f in font_counter.keys() if f > most_common_font], reverse=True)
    font_to_level = {font: level + 1 for level, font in enumerate(filtered_fonts)}

    # 4. 최종 TOC 구성
    toc = []
    for text, font_size, page in font_texts:
        if font_size in font_to_level:
            level = font_to_level[font_size]
            # 숫자나 특수문자로만 된 텍스트 제외
            if any(c.isalpha() for c in text):
                toc.append((level, text, page))

    return toc

def print_toc(toc):
    for level, title, page in toc:
        indent = "  " * (level - 1)  # 레벨에 따른 들여쓰기
        print(f"{indent}└─ {title} (Page {page})")

def extract_contents_toc(doc):
    contents_page = None
    contents_end = None
    toc = []

    # 1. Contents 페이지 찾기
    for i in range(len(doc)):
        text = doc.load_page(i).get_text()
        if "Contents" in text and contents_page is None:
            contents_page = i
        # Contents 다음 섹션 찾기 (Home Preview, Introduction 등)
        elif contents_page is not None and ("Home Preview" in text or "Introduction" in text):
            contents_end = i
            break

    if contents_page is None:
        return []

    if contents_end is None:
        contents_end = contents_page + 2  # Contents가 보통 1-2페이지

    # 2. Contents 페이지에서 목차 추출
    toc_entries = []
    for i in range(contents_page, contents_end):
        page = doc.load_page(i)
        blocks = page.get_text("dict")["blocks"]

        for block in blocks:
            for line in block.get("lines", []):
                spans = line.get("spans", [])
                if not spans:
                    continue

                # 들여쓰기 수준 확인 (첫 번째 span의 bbox로 확인)
                indent_level = 1
                if spans:
                    bbox = spans[0].get("bbox", [0, 0, 0, 0])
                    x0 = bbox[0]  # 왼쪽 좌표
                    if x0 > 100:  # 기본 들여쓰기보다 큰 경우
                        indent_level = 2
                    if x0 > 150:  # 더 큰 들여쓰기
                        indent_level = 3

                # 전체 텍스트 수집
                full_text = ""
                for span in spans:
                    text = span.get("text", "").strip()
                    if text:
                        full_text += text + " "

                full_text = full_text.strip()
                if not full_text or full_text == "Contents":
                    continue

                # 페이지 번호 찾기
                parts = full_text.split()
                if not parts:
                    continue

                try:
                    # 마지막 숫자를 페이지 번호로 사용
                    page_num = None
                    for part in reversed(parts):
                        if part.isdigit():
                            page_num = int(part)
                            break

                    if page_num is None:
                        continue

                    # 제목에서 페이지 번호와 점(...) 제거
                    title = full_text
                    if "." * 3 in title:
                        title = title.split("." * 3)[0].strip()
                    title = " ".join([p for p in title.split() if not p.isdigit() and p != "|"]).strip()

                    if not title:
                        continue

                    toc_entries.append((indent_level, title, page_num))
                except ValueError:
                    continue

    return toc_entries

def extract_pdf_info(file_path):
    doc = fitz.open(file_path)

    # 1. 내장된 TOC 확인
    toc = doc.get_toc()
    if not toc:
        # 2. Contents 섹션 기반 추출 시도
        toc = extract_contents_toc(doc)
        if not toc:
            # 3. 폰트 크기 기반 추출
            toc = guess_pdf_headings(doc)

    structured = []
    id_map = {}  # title -> item
    stack = []   # 계층 추적을 위한 스택

    for level, title, page in toc:
        # title이 None이거나 앞뒤 공백 제거 후 빈 문자열이면 건너뜀
        if title is None or title.strip() == "":
            continue

        node = {
            "id": str(uuid.uuid4()),
            "level": level,
            "title": title,
            "page": page,
            "subKeywords": [],
            "parentId": None,
            "fileName": os.path.basename(file_path),
        }

        # 현재 계층보다 위의 항목은 제거 (스택 정리)
        while stack and stack[-1]["level"] >= level:
            stack.pop()

        if stack:
            parent = stack[-1]
            node["parentId"] = parent["id"]
            parent["subKeywords"].append(node["id"])
        else:
            structured.append(node)

        stack.append(node)
        id_map[node["id"]] = node

    # 평탄화된 리스트 추출
    all_nodes = list(id_map.values())

    print(json.dumps(all_nodes, ensure_ascii=False, indent=2))
    doc.close()

if __name__ == "__main__":
    BASE_DIR = os.path.dirname(os.path.abspath(__file__))
    directory = os.path.join(BASE_DIR, 'static')

    if len(sys.argv) < 2:
        print("❗ 사용법: python pdf_toc_parse.py <파일경로>")
    else:
        extract_pdf_info(sys.argv[1])