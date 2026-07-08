# Home Of Us 部署指南

本文档面向第一版腾讯云轻量服务器部署。推荐系统为 Ubuntu Server 22.04/24.04，服务器只开放 `22`、`80`、`443`。

## 一、服务器准备

服务器已安装 Docker 后，确认：

```bash
docker version
docker compose version
```

如果两个命令都有版本输出，说明 Docker 环境可用。

## 二、方式一：上传源码到服务器构建

这是第一版最省事的方式。

在本机项目根目录打包代码：

```bash
tar --exclude='./node_modules' \
    --exclude='./backend/target' \
    --exclude='./apps/mobile/dist' \
    --exclude='./apps/admin/dist' \
    --exclude='./.git' \
    --exclude='./data' \
    -czf home-of-us.tar.gz .
```

上传到服务器：

```bash
scp home-of-us.tar.gz ubuntu@你的服务器IP:~/
```

在服务器解压：

```bash
mkdir -p ~/home-of-us
tar -xzf ~/home-of-us.tar.gz -C ~/home-of-us
cd ~/home-of-us
```

复制环境变量：

```bash
cp .env.example .env
vim .env
```

至少修改这些密码：

```env
MYSQL_ROOT_PASSWORD=换成强密码
MYSQL_PASSWORD=换成强密码
```

启动：

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

查看状态：

```bash
docker compose -f docker-compose.prod.yml ps
```

访问：

```text
http://你的服务器IP/
http://你的服务器IP/admin/
```

## 三、方式二：本机打镜像后上传到服务器

如果不想把源码放到服务器，可以本机构建镜像，再用 tar 上传。

如果你的 Mac 是 Apple Silicon，必须指定 `linux/amd64`：

```bash
docker buildx build --platform linux/amd64 -f backend/Dockerfile -t home-of-us-backend:latest --load .
docker buildx build --platform linux/amd64 -f deploy/web/Dockerfile -t home-of-us-web:latest --load .
```

打包镜像：

```bash
docker save home-of-us-backend:latest home-of-us-web:latest | gzip > home-of-us-images.tar.gz
tar -czf home-of-us-deploy.tar.gz docker-compose.images.yml .env.example
```

上传：

```bash
scp home-of-us-images.tar.gz home-of-us-deploy.tar.gz ubuntu@你的服务器IP:~/
```

服务器加载镜像：

```bash
mkdir -p ~/home-of-us
tar -xzf ~/home-of-us-deploy.tar.gz -C ~/home-of-us
gunzip -c ~/home-of-us-images.tar.gz | docker load
cd ~/home-of-us
cp .env.example .env
vim .env
docker compose -f docker-compose.images.yml up -d
```

## 四、常用运维命令

查看容器：

```bash
docker compose -f docker-compose.prod.yml ps
```

查看日志：

```bash
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs -f web
docker compose -f docker-compose.prod.yml logs -f mysql
```

重启：

```bash
docker compose -f docker-compose.prod.yml restart
```

停止：

```bash
docker compose -f docker-compose.prod.yml down
```

更新源码部署：

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

## 五、备份

数据库备份：

```bash
docker exec home-of-us-mysql mysqldump -uroot -p home_of_us > home_of_us.sql
```

上传图片目录备份：

```bash
tar -czf uploads-backup.tar.gz data/uploads
```

## 六、后续接域名和 HTTPS

公网 IP 跑通后，再做域名和 HTTPS：

1. 域名解析到服务器公网 IP。
2. 腾讯云防火墙保持开放 `80` 和 `443`。
3. 使用 Nginx + Let's Encrypt 证书。

大陆服务器绑定域名通常需要备案；没有备案前先用公网 IP 测试。
