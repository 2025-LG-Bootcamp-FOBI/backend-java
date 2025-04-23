import pymupdf
import re

def guess_toc_from_text(pages):
    toc = []
    toc_pattern = re.compile(r"^(\d+(\.\d+)*)\s+(.+?)\.{3,}\s+(\d+)$")  # 예: 1.2 계약 해지 ... 3

    for page_num, text in enumerate(pages[:10]):  # 보통 앞쪽 5페이지에 목차 있음
        lines = text.splitlines()
        for line in lines:
            match = toc_pattern.match(line.strip())
            if match:
                number = match.group(1)
                title = match.group(3).strip()
                page = int(match.group(4))
                level = number.count(".") + 1  # 챕터 깊이 추정
                toc.append([level, title, page])
    return toc


doc = pymupdf.open("kakaobank.pdf")
pages = [page.get_text() for page in doc]
# print(pages)
guessed_toc = guess_toc_from_text(pages)
print(guessed_toc)

for level, title, page in guessed_toc:
    indent = "  " * (level - 1)
    print(f"{indent}- {title} (page {page})")