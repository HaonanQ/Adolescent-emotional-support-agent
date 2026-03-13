create table emotion_diary
(
    id          bigint auto_increment comment '日记ID'
        primary key,
    user_id     bigint                             not null comment '用户ID',
    mood        varchar(50)                        not null comment '情绪（开心/难过/愤怒/焦虑/平静/其他）',
    mood_score  int                                not null comment '情绪分数（1-10）',
    content     text                               null comment '日记内容（支持Markdown）',
    image_url   varchar(500)                       null comment '图片URL',
    create_time datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    is_deleted  tinyint  default 0                 null comment '逻辑删除：0-未删除，1-已删除'
)
    comment '情绪日记表';

create index idx_create_time
    on emotion_diary (create_time);

create index idx_user_id
    on emotion_diary (user_id);

create table knowledge_base
(
    id             bigint auto_increment comment '主键ID'
        primary key,
    name           varchar(100)                        not null comment '知识库名称',
    description    text                                null comment '知识库描述',
    table_name     varchar(100)                        not null comment '向量数据库表名',
    status         tinyint   default 1                 null comment '状态：0-停用，1-启用',
    auto_load      tinyint   default 0                 null comment '启动时自动加载：0-否，1-是',
    document_count int       default 0                 null comment '文档数量',
    last_sync_time timestamp                           null comment '最后同步时间',
    create_time    timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    update_time    timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete      tinyint   default 0                 null comment '是否删除：0-否，1-是'
)
    comment '知识库管理表';

create index idx_kb_auto_load
    on knowledge_base (auto_load);

create index idx_kb_status
    on knowledge_base (status);

create table knowledge_document
(
    id                bigint                              not null comment '主键ID'
        primary key,
    knowledge_base_id bigint                              not null comment '所属知识库ID',
    file_name         varchar(255)                        not null comment '文件名称',
    file_path         varchar(500)                        not null comment '文件存储路径',
    file_type         varchar(50)                         null comment '文件类型：markdown/pdf/txt等',
    file_size         bigint                              null comment '文件大小（字节）',
    status            tinyint   default 1                 null comment '状态：0-停用，1-启用',
    chunk_count       int       default 0                 null comment '切分后的文档块数量',
    last_sync_time    timestamp                           null comment '最后同步时间',
    create_time       timestamp default CURRENT_TIMESTAMP null comment '创建时间',
    update_time       timestamp default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间',
    is_delete         tinyint   default 0                 null comment '是否删除：0-否，1-是'
)
    comment '知识库文档表';

create index idx_kd_knowledge_base_id
    on knowledge_document (knowledge_base_id);

create index idx_kd_status
    on knowledge_document (status);

create table user
(
    id                  bigint auto_increment comment '主键ID'
        primary key,
    username            varchar(20)                           not null comment '用户名',
    nickname            varchar(20) default '开心麻花'        null comment '用户昵称',
    password            varchar(100)                          null comment '密码（BCrypt加密）',
    relationship_status tinyint     default 0                 null comment '恋爱状态：0-单身，1-恋爱中',
    create_time         datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time         datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    is_deleted          tinyint     default 0                 not null comment '逻辑删除：0-未删除，1-已删除',
    is_admin            int         default 0                 not null comment '是否为管理员，0 -- 不是，1 -- 是，默认不是',
    constraint idx_username
        unique (username)
)
    comment '用户表';

create table userfile
(
    id          bigint auto_increment comment '文件主键ID'
        primary key,
    file_url    varchar(255)                         not null comment '文件访问URL',
    user_id     bigint                               not null comment '关联用户ID',
    file_name   varchar(100)                         not null comment '原始文件名',
    is_deleted  tinyint(1) default 0                 null comment '逻辑删除标记(0-未删除 1-已删除)',
    create_time datetime   default CURRENT_TIMESTAMP not null comment '创建时间',
    update_time datetime   default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '修改时间'
)
    comment '文件存储表';

create index idx_user_id
    on userfile (user_id)
    comment '用户ID索引';


