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

create JWT_SECRECT KEY for prod using this command
JWT_SECRET=<base64 32-byte> (e.g. from **openssl rand -base64 32**)
