-- 财富资产类型：增加排序字段并初始化 sort_num（数值越小越靠前）
-- 若 sort_num 列已存在，可跳过 ALTER 语句，仅执行 UPDATE 部分

ALTER TABLE wealth_type
  ADD COLUMN sort_num INT DEFAULT NULL COMMENT '排序' AFTER remark;

UPDATE wealth_type SET sort_num = 1 WHERE asset_type = '支付宝';
UPDATE wealth_type SET sort_num = 2 WHERE asset_type = '微信';
UPDATE wealth_type SET sort_num = 3 WHERE asset_type = '现金';
UPDATE wealth_type SET sort_num = 4 WHERE asset_type = '基金';
UPDATE wealth_type SET sort_num = 5 WHERE asset_type = '银行卡';
UPDATE wealth_type SET sort_num = 6 WHERE asset_type = '公积金';
UPDATE wealth_type SET sort_num = 7 WHERE asset_type = '黄金';
UPDATE wealth_type SET sort_num = 8 WHERE asset_type = '债权';
