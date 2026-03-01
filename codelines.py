import os

# 排除的目录和文件后缀
EXCLUDE_DIRS = {'.git', 'target', 'node_modules', 'venv', 'dist', 'build'}
EXCLUDE_EXTENSIONS = {'.class', '.jar', '.ttf', '.otf', '.exe', '.node', '.pack', '.idx', '.rev', '.original', '.png', '.jpg', '.gif','.yaml','.yml','.json'}

def count_lines_in_file(file_path):
    code_lines = 0
    comment_lines = 0
    empty_lines = 0
    in_block_comment = False

    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            for line in f:
                stripped_line = line.strip()
                if not stripped_line:
                    empty_lines += 1
                    continue
                
                # 处理注释逻辑（兼容Java/JS/TS/Vue/CSS等）
                file_ext = os.path.splitext(file_path)[1].lower()
                if file_ext in ('.java', '.js', '.ts', '.vue', '.jsx', '.tsx'):
                    if stripped_line.startswith("//"):
                        comment_lines += 1
                    elif stripped_line.startswith("/*"):
                        comment_lines += 1
                        in_block_comment = True
                    elif stripped_line.endswith("*/") and in_block_comment:
                        comment_lines += 1
                        in_block_comment = False
                    elif in_block_comment:
                        comment_lines += 1
                    else:
                        code_lines += 1
                elif file_ext == '.vue':
                    # Vue文件特殊处理（简单版，如需精准可解析模板）
                    if stripped_line.startswith("//") or stripped_line.startswith("<!--"):
                        comment_lines += 1
                    else:
                        code_lines += 1
    except Exception as e:
        # 静默失败，不打印错误（如需调试可取消注释）
        # print(f"读取文件失败 {file_path}: {e}")
        return 0, 0, 0

    return code_lines, comment_lines, empty_lines

def count_lines_in_project(directory):
    # 前端文件类型
    FRONTEND_EXTS = ('.js', '.vue', '.ts', '.jsx', '.tsx', '.css', '.scss', '.html')
    # 后端文件类型
    BACKEND_EXTS = ('.java')

    # 初始化统计
    fe_code, fe_comment, fe_empty = 0, 0, 0
    be_code, be_comment, be_empty = 0, 0, 0

    for root, dirs, files in os.walk(directory):
        # 排除指定目录
        dirs[:] = [d for d in dirs if d not in EXCLUDE_DIRS]
        
        for file in files:
            # 排除二进制文件
            file_ext = os.path.splitext(file)[1].lower()
            if file_ext in EXCLUDE_EXTENSIONS:
                continue

            file_path = os.path.join(root, file)
            code, comment, empty = count_lines_in_file(file_path)

            # 区分前后端统计
            if file_ext in FRONTEND_EXTS:
                fe_code += code
                fe_comment += comment
                fe_empty += empty
            elif file_ext in BACKEND_EXTS:
                be_code += code
                be_comment += comment
                be_empty += empty

    # 汇总总数
    total_code = fe_code + be_code
    total_comment = fe_comment + be_comment
    total_empty = fe_empty + be_empty

    return {
        "frontend": (fe_code, fe_comment, fe_empty),
        "backend": (be_code, be_comment, be_empty),
        "total": (total_code, total_comment, total_empty)
    }

if __name__ == "__main__":
    project_directory = "."
    result = count_lines_in_project(project_directory)

    print("=" * 60)
    print("前端代码统计（JS/Vue/TS/CSS等）：")
    print(f"  代码行数: {result['frontend'][0]}")
    print(f"  注释行数: {result['frontend'][1]}")
    print(f"  空行数: {result['frontend'][2]}")
    print(f"  前端总行数: {sum(result['frontend'])}")

    print("\n后端代码统计：")
    print(f"  代码行数: {result['backend'][0]}")
    print(f"  注释行数: {result['backend'][1]}")
    print(f"  空行数: {result['backend'][2]}")
    print(f"  后端总行数: {sum(result['backend'])}")

    print("\n项目总统计：")
    print(f"  总代码行数: {result['total'][0]}")
    print(f"  总注释行数: {result['total'][1]}")
    print(f"  总空行数: {result['total'][2]}")
    print(f"  项目总行数: {sum(result['total'])}")
    print("=" * 60)