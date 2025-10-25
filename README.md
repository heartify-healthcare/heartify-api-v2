# How to run
## Clone the repository for your life
```bash
git clone https://github.com/votranphi/heartistry-api-v2
cd heartistry-api-v2
git submodule update --init --recursive
```

## Run the project for your life
```bash
# In config-server folder
cd config-server
mvn clean compile
mvn spring-boot:run

# In eureka-server folder
cd eureka-server
mvn clean compile
mvn spring-boot:run

# api-gateway folder
cd api-gateway
mvn clean compile
mvn spring-boot:run

# user-service folder
cd user-service
mvn clean compile
mvn spring-boot:run

# ai-service folder
cd ai-service
mvn clean compile
mvn spring-boot:run
```

## Update the config-repo if there's changes on the actual config-repo
```bash
cd config-repo
git pull origin main
cd ..
git add config-repo
git commit -m "chore(branch-name): update config submodule"
git push
```

## Database-related commands
PosgreSQL
```bash
psql -h localhost -p 5432 -d heartify_db -U heartify
select * from "users"
select * from "otps"
```

MongoDB
```bash
mongosh "mongodb://heartify:heartify@localhost:27017/heartify_ai_db?authSource=admin"
db.ecg_sessions.find().limit(10)
db.ecg_recordings.find().limit(10)
db.predictions.find().limit(10)
db.explanations.find().limit(10)
```