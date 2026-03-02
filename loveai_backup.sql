mysqldump: [Warning] Using a password on the command line interface can be insecure.
-- MySQL dump 10.13  Distrib 8.0.43, for macos15 (arm64)
--
-- Host: localhost    Database: loveai
-- ------------------------------------------------------
-- Server version	8.0.43

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `product`
--

DROP TABLE IF EXISTS `product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `product_name` varchar(100) NOT NULL COMMENT '商品名称',
  `category` varchar(50) NOT NULL COMMENT '商品分类：书籍/礼物/课程',
  `description` text COMMENT '商品描述',
  `price` decimal(10,2) NOT NULL COMMENT '价格',
  `image_url` varchar(500) DEFAULT NULL COMMENT '商品图片URL（MinIO）',
  `jump_url` varchar(500) NOT NULL COMMENT '跳转链接（淘宝/京东/自营）',
  `tags` varchar(200) DEFAULT NULL COMMENT '标签（逗号分隔）：道歉,生日,纪念日,表白,学习',
  `scene` varchar(200) DEFAULT NULL COMMENT '适用场景（逗号分隔）：吵架,纪念日,初次见面,冷战',
  `status` tinyint DEFAULT '1' COMMENT '状态：1上架 0下架',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `product`
--

LOCK TABLES `product` WRITE;
/*!40000 ALTER TABLE `product` DISABLE KEYS */;
INSERT INTO `product` VALUES (1,'《亲密关系》','书籍','深度解析恋爱心理学经典著作，帮助理解和建立健康的亲密关系',49.80,'https://your-minio.com/products/book-qinmi.jpg','https://s.taobao.com/search?q=亲密关系','心理学,沟通技巧','吵架,冷战,学习提升',1,'2026-02-22 12:25:37','2026-02-22 12:25:37'),(2,'星空投影灯','礼物','浪漫星空投影灯，营造温馨氛围，适合纪念日送礼',129.00,'https://your-minio.com/products/star-light.jpg','https://item.jd.com/100012345678.html','浪漫,纪念日,惊喜','纪念日,生日,表白,道歉',1,'2026-02-22 12:25:37','2026-02-22 12:25:37'),(3,'定制刻字情侣手链','礼物','925银情侣手链，可刻字定制，精美礼盒包装',299.00,'https://your-minio.com/products/bracelet.jpg','https://s.taobao.com/search?q=情侣手链刻字','定制,纪念品,仪式感','纪念日,生日,表白',1,'2026-02-22 12:25:37','2026-02-22 12:25:37');
/*!40000 ALTER TABLE `product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user`
--

DROP TABLE IF EXISTS `user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(20) NOT NULL COMMENT '用户名',
  `password` varchar(100) DEFAULT NULL COMMENT '密码（BCrypt加密）',
  `relationship_status` tinyint DEFAULT '0' COMMENT '恋爱状态：0-单身，1-恋爱中',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2025475557931888643 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user`
--

LOCK TABLES `user` WRITE;
/*!40000 ALTER TABLE `user` DISABLE KEYS */;
INSERT INTO `user` VALUES (2025475557931888642,'qhnan','12345678',0,'2026-02-22 15:39:39','2026-02-22 15:39:39',0);
/*!40000 ALTER TABLE `user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `userfile`
--

DROP TABLE IF EXISTS `userfile`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `userfile` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '文件主键ID',
  `file_url` varchar(255) NOT NULL COMMENT '文件访问URL',
  `user_id` bigint NOT NULL COMMENT '关联用户ID',
  `file_name` varchar(100) NOT NULL COMMENT '原始文件名',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标记(0-未删除 1-已删除)',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`) COMMENT '用户ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件存储表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `userfile`
--

LOCK TABLES `userfile` WRITE;
/*!40000 ALTER TABLE `userfile` DISABLE KEYS */;
/*!40000 ALTER TABLE `userfile` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-03-02 15:12:57
