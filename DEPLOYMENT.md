# AWS Deployment Guide — XIRR Calculator

Deploy the XIRR Calculator on an EC2 t2.micro spot instance in Mumbai (ap-south-1).

**Estimated cost:** ~₹100–150/month

---

## Prerequisites

- AWS account
- Java 21 and Maven installed locally
- Built jar: `mvn clean package -DskipTests`

---

## 1. Launch EC2 Spot Instance

1. Go to [EC2 Console](https://console.aws.amazon.com/ec2/) → **Mumbai (ap-south-1)**
2. Click **Launch instances**
3. Configure:
   - **Name:** `xirr-calculator`
   - **AMI:** Amazon Linux 2023
   - **Instance type:** `t2.micro`
   - **Key pair:** Create new → name `xirr-key` → RSA → `.pem` → Download
   - **Network settings → Edit:**
     - Auto-assign public IP: **Enable**
     - Security group: Create new
     - Rule 1: SSH → Port 22 → Source: **My IP**
     - Rule 2: Custom TCP → Port 8080 → Source: **Anywhere (0.0.0.0/0)**
   - **Advanced details:**
     - Purchasing option: ✅ **Request Spot Instances**
   - **Storage:** 8 GB gp3
4. Click **Launch instance**

## 2. Attach Elastic IP (keeps IP fixed)

1. Go to **EC2 → Elastic IPs**
2. Click **Allocate Elastic IP address**
3. Click **Associate** → select your instance
4. Free while attached to a running instance

## 3. Fix Key Permissions (Windows)

```powershell
icacls "E:\path\to\xirr-key.pem" /inheritance:r /grant:r "$($env:USERNAME):R"
```

## 4. Upload the Jar

Replace `YOUR_IP` with your instance's public IPv4:

```powershell
scp -i "E:\path\to\xirr-key.pem" "e:\xirr\target\xirr-calculator-0.0.1-SNAPSHOT.jar" ec2-user@YOUR_IP:~/
```

## 5. SSH into the Instance

```powershell
ssh -i "E:\path\to\xirr-key.pem" ec2-user@YOUR_IP
```

## 6. Server Setup

```bash
# Install Java
sudo yum install java-21-amazon-corretto -y

# Create app directory and move jar
mkdir ~/xirr
mv ~/xirr-calculator-0.0.1-SNAPSHOT.jar ~/xirr/
```

## 7. Create .env File

```bash
nano ~/xirr/.env
```

Paste the following (replace with your actual values):

```
MAIL_HOST=smtp.zoho.in
MAIL_PORT=465
MAIL_USERNAME=your-email@example.com
MAIL_PASSWORD=your-app-password
MAIL_SSL=true
MAIL_STARTTLS=false
ACCESS_REQUEST_NOTIFY_EMAIL=your-email@example.com
```

Save with **Ctrl+O → Enter**, exit with **Ctrl+X**, then:

```bash
chmod 600 ~/xirr/.env
```

## 8. Create systemd Service

```bash
sudo tee /etc/systemd/system/xirr.service > /dev/null << 'EOF'
[Unit]
Description=XIRR Calculator
After=network.target

[Service]
User=ec2-user
WorkingDirectory=/home/ec2-user/xirr
EnvironmentFile=/home/ec2-user/xirr/.env
ExecStart=/usr/bin/java -jar xirr-calculator-0.0.1-SNAPSHOT.jar
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
```

## 9. Start the App

```bash
sudo systemctl daemon-reload
sudo systemctl enable xirr
sudo systemctl start xirr
```

Verify:

```bash
sudo systemctl status xirr
```

Open in browser: `http://YOUR_IP:8080`

---

## Useful Commands

```bash
# View live logs
journalctl -u xirr -f

# Restart app
sudo systemctl restart xirr

# Stop app
sudo systemctl stop xirr
```

## Redeploy After Code Changes

On your local machine:

```powershell
mvn clean package -DskipTests
scp -i "E:\path\to\xirr-key.pem" "e:\xirr\target\xirr-calculator-0.0.1-SNAPSHOT.jar" ec2-user@YOUR_IP:~/xirr/
```

On the server:

```bash
sudo systemctl restart xirr
```

## Notes

- **Don't stop/terminate** the instance from EC2 console — that shuts down the server
- Closing the SSH terminal on your local machine does **not** affect the running app
- The Elastic IP is free only while attached to a running instance — release it if you terminate the instance
- Spot instances can be interrupted by AWS (rare for t2.micro) — the systemd service auto-restarts the app when the instance comes back
