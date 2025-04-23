from docx import Document
import win32com.client
import os

def convert_doc_to_docx(doc_file, docx_file):
    # 경로 확인 (파일이 존재하는지 확인)
    if not os.path.exists(doc_file):
        print(f"지정된 DOC 파일이 존재하지 않습니다: {doc_file}")
        return

    # Word 애플리케이션 실행
    word = win32com.client.Dispatch("Word.Application")
    word.Visible = False  # Word 창을 표시하지 않음

    try:
        # DOC 파일 열기
        doc = word.Documents.Open(doc_file)

        # DOCX로 저장
        doc.SaveAs(docx_file, FileFormat=16)  # 16은 .docx 형식
        print(f"파일이 성공적으로 저장되었습니다: {docx_file}")

        doc.Close()  # 파일 닫기
    except Exception as e:
        print(f"파일을 변환하는 중 오류가 발생했습니다: {e}")
    finally:
        word.Quit()  # Word 애플리케이션 종료


def extract_headings(docx_file):
    # Word 문서 열기
    doc = Document(docx_file)

    headings = []

    # 각 단락을 검사하여 헤딩 스타일이 있는 경우 추출
    for para in doc.paragraphs:
        if para.style.name.startswith('Heading'):
            level = int(para.style.name.split()[-1])  # 'Heading 1', 'Heading 2' 등에서 레벨 추출
            headings.append((level, para.text))

    return headings

def print_toc(headings):
    # 목차 출력
    for level, text in headings:
        indent = ' ' * (level - 1) * 4  # 레벨에 맞춰 들여쓰기
        print(f"{indent}{text}")

# .doc 파일을 .docx로 변환
# doc_file = './system_design_document_template.doc'
# docx_file = './system_design_document_template.docx'
doc_file = r'C:\Users\User\Desktop\Data\system_design_document_template.doc'
docx_file = r'C:\Users\User\Desktop\Data\system_design_document_template.docx'
convert_doc_to_docx(doc_file, docx_file)

# .docx 파일에서 목차 추출
headings = extract_headings(docx_file)
print_toc(headings)