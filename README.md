### Author Firat Faruk Eren, firate.eren[at]gmail.com

## Design
Billing Service has a scheduler which runs periodically and sleeps for one hour delay. The Scheduler selects all pending invoices with
past startDates and charge them concurrently(implemented by Kotlin coroutines). If we want an invoice to get charged on the 1th of the Month then
start date should be 1th of the Month.  

I did lots of research to implement a generic retry logic for billing service. There are some errors like network error 
which we should charge again in minutes and there are some errors which we should charge one day later. There should be limits
to retry failed charges. I decided to implement a generic retry logic dependent to payment status. 
Every payment status has an attempt count and delay value. If the number of invoice charge attempts for a payment status 
exceed the attempt count then this invoice will not be charged anymore otherwise delay invoice's start date for future attempts. All invoice charge attempts
will be saved in InvoicePaymentTable. 

## Limitations
BillingService is singleton but it's not thread-safe. if multiple instance of application run simultaneously and with same database it will be unstable.


 




## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will pay those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

### Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── pleo-antaeus-app
|
|       Packages containing the main() application. 
|       This is where all the dependencies are instantiated.
|
├── pleo-antaeus-core
|
|       This is where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|
|       Module interfacing with the database. Contains the models, mappings and access layer.
|
├── pleo-antaeus-models
|
|       Definition of models used throughout the application.
|
├── pleo-antaeus-rest
|
|        Entry point for REST API. This is where the routes are defined.
└──
```

## Instructions
Fork this repo with your solution. We want to see your progression through commits (don’t commit the entire solution in 1 step) and don't forget to create a README.md to explain your thought process.

Happy hacking 😁!

## How to run
```
./docker-start.sh
```

## Libraries currently in use
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
