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
                if font_size > 0 and 5 < len(text) < 100:
                    font_texts.append((text, font_size, i + 1))
                    font_counter[font_size] += 1

    if not font_counter:
        return []

    # 2. 가장 많이 등장한 폰트 사이즈 찾기
    most_common_font, _ = font_counter.most_common(1)[0]

    # 3. 가장 많이 등장한 폰트 초과만 사용하여 레벨 매핑
    filtered_fonts = [f for f in font_counter.keys() if f > most_common_font]
    sorted_fonts = sorted(filtered_fonts, reverse=True)
    font_to_level = {font: level + 1 for level, font in enumerate(sorted_fonts)}

    # 4. 최종 TOC 구성
    guessed_toc = []
    for text, font_size, page in font_texts:
        level = font_to_level.get(font_size, len(sorted_fonts))  # 매핑 없으면 최하위 레벨
        guessed_toc.append((level, text, page))

    return guessed_toc

def extract_pdf_info(file_path):
    doc = fitz.open(file_path)
    toc = doc.get_toc()
    if not toc:
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
            "fileName": file_path.split("/")[-1],
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