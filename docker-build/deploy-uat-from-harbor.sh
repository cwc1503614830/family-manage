#!/usr/bin/env bash
#
# uat 环境部署入口（UAT，外部用户使用）。
# 宿主机端口：前端 8092 -> 容器 8091，后端 12301 -> 容器 12300。
# 与 prd 同机共存，容器名 / 网络名 / 端口全部错开，互不影响。
# 数据库由 application-uat.properties 决定（family_manage_uat），
# MinIO bucket 为 family-manage-uat。
#
# 用法：
#   ./deploy-uat-from-harbor.sh            # 部署 latest
#   ./deploy-uat-from-harbor.sh v1.2.0     # 部署指定 tag（回滚也用它）
#   DB_URL='jdbc:mysql://<host>:3306/family_manage_uat?...' ./deploy-uat-from-harbor.sh
#
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
export STACK_ENV=uat
exec "${HERE}/deploy-from-harbor.sh" "$@"
