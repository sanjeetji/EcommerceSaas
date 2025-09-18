E-commerce SaaS Platform Description
This is a robust, multi-tenant e-commerce Software-as-a-Service (SaaS) platform built using Spring Boot. The application is designed to efficiently handle core e-commerce functionalities while supporting multiple clients with isolated data and customizable configurations. Key features include:

**Multi-Tenancy:** Seamlessly supports multiple clients, each with dedicated data storage and tailored settings, ensuring scalability and data privacy.
**Core E-commerce Features:** Manages product catalogs, inventory, orders, payments, and customer accounts for each client.
**Scalable Architecture:** Leverages Spring Boot's modular framework to ensure high performance, maintainability, and extensibility.
**Customizable Workflows:** Provides flexible configurations to adapt to diverse client requirements, such as pricing models, promotions, and shipping rules.
**Secure and Reliable:** Implements industry-standard security practices for data protection, authentication, and authorization across tenants.
**API-Driven:** Offers RESTful APIs for seamless integration with third-party systems, including payment gateways, logistics, and analytics tools.

This platform is ideal for businesses seeking a scalable, customizable, and efficient solution to power their e-commerce operations.


**How to run with profiles**
**Dev (IntelliJ or CLI):**
VM option: **-Dspring.profiles.active=dev**
or environment: **SPRING_PROFILES_ACTIVE=dev**

**Prod:**
**SPRING_PROFILES_ACTIVE=prod**

Why these changes
The common file no longer enables Redis or sets JWT defaults—so dev and prod are cleanly separated.
Dev disables Redis auto-config entirely (no connection attempts, green health).
Prod expects JWT_SECRET and uses Redis; health will go DOWN only if real issues occur (e.g., Redis unavailable).
Rotation keys (jwt.rotation.*) are only meaningful when jwt.rotation.enabled=true and jwt.secret is not set.
Set JWT_SECRET to a base64 32-byte value (e.g., openssl rand -base64 32)
Make sure Redis is reachable at the configured host/port


**How to check what’s active**
Logs at startup:
When the app boots, Spring logs something like:
No active profile set, falling back to 1 default profile: "default"
or
The following profiles are active: dev

**Actuator (if enabled):**
Call:
GET http://localhost:8080/actuator/env
and search for spring.profiles.active



**How to set SPRING_PROFILES_ACTIVE**
1. Command line
java -jar ecommerce.jar --spring.profiles.active=dev

**2. Environment variable**

**In Linux/macOS:**
export SPRING_PROFILES_ACTIVE=dev


**In Windows (PowerShell):**
$env:SPRING_PROFILES_ACTIVE="dev"
3. IntelliJ IDEA / Eclipse Run Config

Edit Run Configuration → Environment variables → add
SPRING_PROFILES_ACTIVE=dev

4. application.properties (fallback, not recommended for prod)
Add this line:
spring.profiles.active=dev

⚠️ This hardcodes the profile, so it overrides dynamic environment settings.
✅ So: in dev machine, set SPRING_PROFILES_ACTIVE=dev.
On production server, set SPRING_PROFILES_ACTIVE=prod

**To check health of the server**
http://localhost:8080/actuator/health

1. create JWT_SECRECT KEY for prod using this command
JWT_SECRET=<base64 32-byte> (e.g. from **openssl rand -base64 32**)



1. Please set the environment variable as per running environment

#For Dev Profile:
#export SPRING_PROFILES_ACTIVE=dev
#mvn spring-boot:run
#For Prod Profile:
#export SPRING_PROFILES_ACTIVE=prod
#mvn spring-boot:run

Option 2: Set via application.properties
Alternatively, in local dev, you can directly set the profile in application.properties:
# application.properties
spring.profiles.active=dev


This will be ignored if you pass the SPRING_PROFILES_ACTIVE variable via the command line, but it’s a useful fallback.


2. Railway (Production Environment)
For Railway (or any production environment), you’ll generally set environment variables through their dashboard or CLI.
Option 1: Set via Railway Dashboard
Go to your Railway project dashboard.

Under the Environment Variables section, add the following:
Key: SPRING_PROFILES_ACTIVE
Value: prod
This will ensure that prod settings are used when you deploy the app.
Option 2: Set via Railway CLI
If you're using the Railway CLI to deploy, you can set environment variables before pushing your app:
railway variables set SPRING_PROFILES_ACTIVE=prod
Then push your changes to Railway.


Prod (Railway / Docker / VM):
Export these (example):
export SPRING_PROFILES_ACTIVE=prod
export MYSQLHOST=maglev.proxy.rlwy.net
export MYSQLPORT=10939
export MYSQLDATABASE=ecommerce_db
export MYSQLUSER=root
export MYSQLPASSWORD=superSecret
export JWT_SECRET=base64_or_long_random_string
export APP_SESSION_STORE=db
export APP_LOG_LEVEL=INFO

Then:
java -jar app.jar
# or
mvn spring-boot:run
