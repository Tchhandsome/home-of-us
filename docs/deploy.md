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

在本机项目根目录执行：

```bash
sh deploy/scripts/package-source.sh
```

上传到服务器：

```bash
scp deploy/dist/home-of-us-source.tar.gz ubuntu@你的服务器IP:~/
```

在服务器解压：

```bash
mkdir -p ~/home-of-us
tar -xzf ~/home-of-us-source.tar.gz -C ~/home-of-us
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

在本机项目根目录执行：

```bash
sh deploy/scripts/package-images.sh
```

脚本会先在本机执行后端和前端构建，然后把构建产物打进镜像。默认构建 `linux/amd64` 镜像，适配腾讯云轻量服务器。如果 Docker Desktop 没有启动，先启动 Docker Desktop 再执行。

生成的镜像包包含：

- `home-of-us-backend:latest`
- `home-of-us-web:latest`
- `mysql:8.0`

上传：

```bash
scp deploy/dist/home-of-us-images.tar.gz deploy/dist/home-of-us-deploy.tar.gz ubuntu@你的服务器IP:~/
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

## 七、重新发版最短路径

如果服务器已经跑起来了，后续大多数情况直接走“镜像包重新发版”即可。

### 7.1 本机重新打包

在项目根目录执行：

```bash
sh deploy/scripts/package-images.sh
```

脚本会自动完成：

1. 后端打包。
2. 手机端构建。
3. 管理端构建。
4. 生成两个文件：
   - `deploy/dist/home-of-us-images.tar.gz`
   - `deploy/dist/home-of-us-deploy.tar.gz`

### 7.2 上传到服务器

```bash
scp deploy/dist/home-of-us-images.tar.gz deploy/dist/home-of-us-deploy.tar.gz ubuntu@你的服务器IP:~/
```

### 7.3 服务器重新加载并重启

```bash
mkdir -p ~/home-of-us
tar -xzf ~/home-of-us-deploy.tar.gz -C ~/home-of-us
gunzip -c ~/home-of-us-images.tar.gz | docker load
cd ~/home-of-us
cp -n .env.example .env
docker compose -f docker-compose.images.yml up -d --force-recreate
```

### 7.4 发版后检查

```bash
docker compose -f docker-compose.images.yml ps
docker compose -f docker-compose.images.yml logs -f backend
docker compose -f docker-compose.images.yml logs -f web
```

如果只是日常功能更新，按上面 4 步走就够了。
