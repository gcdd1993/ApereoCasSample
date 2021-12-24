CREATE DATABASE cas_sample;

CREATE TABLE customer_user
(
    id        int UNSIGNED PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username  varchar(20)  NOT NULL COMMENT '用户名',
    password  varchar(255) NOT NULL COMMENT '密码',
    sex       char(1)      NOT NULL COMMENT '性别',
    married   boolean      NOT NULL COMMENT '婚否',
    education tinyint      NOT NULL COMMENT '学历：1大专、2本科、3研究生、4博士、5其他',
    tel       char(11) COMMENT '电话号码',
    email     varchar(200) COMMENT '邮箱',
    address   varchar(200) COMMENT '住址',
    disabled  boolean      NOT NULL DEFAULT FALSE COMMENT '是否禁用',
    expired   boolean      NOT NULL DEFAULT FALSE COMMENT '是否过期'
);

INSERT INTO customer_user (username, password, sex, married, education, tel, email, address)
VALUES ('zhangsan', 'f0147cd7661ca1f3c68b660d3b7fb0ed4c56f45ffaf3fb17fa967b5e1031eedfc9e8b64622b2844b', '男', FALSE, 1,
        '17715182604', 'zhangsan@163.com', '北京市');
INSERT INTO customer_user (username, password, sex, married, education, tel, email, address)
VALUES ('lisi', 'f0147cd7661ca1f3c68b660d3b7fb0ed4c56f45ffaf3fb17fa967b5e1031eedfc9e8b64622b2844b', '男', FALSE, 1,
        '17715182605', 'zhangsan@163.com', '北京市');
INSERT INTO customer_user (username, password, sex, married, education, tel, email, address)
VALUES ('wangwu', 'f0147cd7661ca1f3c68b660d3b7fb0ed4c56f45ffaf3fb17fa967b5e1031eedfc9e8b64622b2844b', '男', FALSE, 1,
        '17715182606', 'zhangsan@163.com', '北京市');