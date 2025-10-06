Öncelikle paket listesini güncelle ve R’yi yükle:

sudo apt update
sudo apt upgrade -y

----------------------------------------------------------------------------------

Sonra R’yi yükle:

sudo apt install -y r-base r-base-dev

Kontrol:

R --version 

# Lattice kütüphanesini kur
R -e "install.packages('lattice', repos='http://cran.rstudio.com/')"

# Kurulumu test et
R -e "library(lattice); print('Lattice loaded successfully!')"

# Eğer hata alırsan bağımlılıkları kur
sudo apt install -y libcurl4-openssl-dev libssl-dev libxml2-dev

----------------------------------------------------------------------------------

RStudio Server Kurulumu (WSL içinde çalıştırabilmek için)

sudo apt update
sudo apt install -y gdebi-core

wget https://s3.amazonaws.com/rstudio-ide-build/server/jammy/amd64/rstudio-server-2025.09.0-379-amd64.deb

sudo gdebi rstudio-server-2025.09.0-379-amd64.deb


Servisi başlat / kontrol et:

sudo systemctl enable rstudio-server
sudo systemctl start rstudio-server
sudo systemctl status rstudio-server

Sonra tarayıcıdan:
👉 http://localhost:8787

Burada Linux kullanıcı adın ve şifrenle giriş yapabilirsin.

----------------------------------------------------------------------------------

Mongodb kurlumu

# 1. Sistem paketlerini güncelle
sudo apt update
sudo apt upgrade -y

# 2. MongoDB GPG anahtarını içe aktar
curl -fsSL https://pgp.mongodb.com/server-7.0.asc | \
   sudo gpg -o /usr/share/keyrings/mongodb-server-7.0.gpg --dearmor

# 3. MongoDB repository'yi ekle
echo "deb [ arch=amd64,arm64 signed-by=/usr/share/keyrings/mongodb-server-7.0.gpg ] https://repo.mongodb.org/apt/ubuntu jammy/mongodb-org/7.0 multiverse" | \
   sudo tee /etc/apt/sources.list.d/mongodb-org-7.0.list

# 4. Paket listesini güncelle
sudo apt update

# 5. MongoDB'yi kur
sudo apt install -y mongodb-org

# 6. Data dizinini oluştur
sudo mkdir -p /data/db
sudo chown -R $USER:$USER /data/db

# 7. MongoDB'yi başlat
sudo systemctl start mongod

# 8. MongoDB'nin çalıştığını kontrol et
sudo systemctl status mongod

# 9. Sistem başladığında otomatik başlaması için
sudo systemctl enable mongod

# MongoDB'nin çalıştığından emin ol
sudo systemctl start mongod
sudo systemctl status mongod
# Download CSV
wget https://raw.githubusercontent.com/ozmen54/SWE307-2025/main/swe307_pro1.csv

# Make sure MongoDB is running
sudo systemctl start mongod

# Import CSV to MongoDB
mongoimport --db swe307 --collection datapoints --type csv --headerline --file swe307_pro1.csv

# Kontrol et
mongosh
use swe307
db.datapoints.countDocuments()
db.datapoints.find().limit(3)
exit

# Verify import
mongosh --eval "use swe307; db.datapoints.countDocuments()"
mongosh --eval "use swe307; db.datapoints.find().limit(3)"

----------------------------------------------------------------------------------
GraalVM ve R Kontrolü

# GraalVM kontrolü
java -version
echo $JAVA_HOME

# Eğer GraalVM değilse:
export JAVA_HOME=/usr/lib/jvm/graalvm-ce-java17-22.3.0
export PATH=$JAVA_HOME/bin:$PATH

# R component kontrolü
gu list

# Eğer R yüklü değilse:
gu install R

# R lattice kütüphanesi kontrolü
R -e "library(lattice)"

# Eğer yüklü değilse:
R -e "install.packages('lattice', repos='http://cran.rstudio.com/')

----------------------------------------------------------------------------------

Projeyi Çalıştırmak

# Maven ile build ve run
mvn clean install
mvn spring-boot:run
