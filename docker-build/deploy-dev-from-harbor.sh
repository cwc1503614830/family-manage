#!/usr/bin/env bash
#
# dev 环境部署入口（开发环境，内部使用）。
# 宿主机端口：前端 8093 -> 容器 8091，后端 12302 -> 容器 12300。
#
# 注意：application-dev.properties 默认指向 192.168.216.140（开发机可达，但不在这台服务器上），
# 因此在本服务器上部署 dev 时，默认改用本机独立库 family_manage_dev / 独立 bucket
# family-manage-dev，避免与 prd（库 family-manage、bucket family-manage）共用同一份数据。
# createDatabaseIfNotExist=true 让 JDBC 自动建库，库空时应用启动会用
# DatabaseInitializer 自动建表并写入默认账号数据，因此无需手工导入 SQL。
#
# 需要改回 216.140 或换库时，部署前显式覆盖即可：
#   DB_URL='jdbc:mysql://192.168.216.140:3306/family_manage?...' ./deploy-dev-from-harbor.sh
#
# 用法：
#   ./deploy-dev-from-harbor.sh            # 部署 latest
#   ./deploy-dev-from-harbor.sh v1.2.0     # 部署指定 tag
#
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
export STACK_ENV=dev

: "${DB_URL:=jdbc:mysql://192.168.236.129:3306/family_manage_dev?createDatabaseIfNotExist=true&useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Hongkong}"
: "${MINIO_ENDPOINT:=http://192.168.236.129:9000}"
: "${MINIO_PUBLIC_URL:=http://192.168.236.129:9000}"
: "${MINIO_BUCKET:=family-manage-dev}"
export DB_URL MINIO_ENDPOINT MINIO_PUBLIC_URL MINIO_BUCKET

exec "${HERE}/deploy-from-harbor.sh" "$@"
