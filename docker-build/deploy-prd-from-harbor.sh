#!/usr/bin/env bash
#
# prd 环境部署入口（生产环境，内部使用）。
# 宿主机端口：前端 8091 -> 容器 8091，后端 12300 -> 容器 12300。
# 具体逻辑全部在 deploy-from-harbor.sh，这里只负责指定环境。
#
# 用法：
#   ./deploy-prd-from-harbor.sh            # 部署 latest
#   ./deploy-prd-from-harbor.sh v1.2.0     # 部署指定 tag（回滚也用它）
#
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
export STACK_ENV=prd
exec "${HERE}/deploy-from-harbor.sh" "$@"
