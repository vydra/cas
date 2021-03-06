---
layout: default
title: CAS - Duo Security Authentication
---

# Duo Security Authentication

Duo Security is a two-step verification service the provides additional security for access to institutional and personal data.  

Duo offers several options for authenticating users:

- a mobile push notification and one-button verification of identity to a smartphone (requires the free Duo Mobile app)
- a one-time code generated on a smartphone
- a one-time code generated by Duo and sent to a handset via SMS text messaging
- a telephone call from that will prompt you to validate the login request

[See here](https://www.duo.com/) for additional information.

```xml
<dependency>
     <groupId>org.apereo.cas</groupId>
     <artifactId>cas-server-support-duo</artifactId>
     <version>${cas.version}</version>
</dependency>
```

You may need to add the following repositories to the WAR overlay:

```xml
<repository>
    <id>duo</id>
    <url>https://dl.bintray.com/uniconiam/maven</url>
</repository>
<repository>
    <id>dupclient</id>
    <url>https://jitpack.io</url>
</repository>
```

## Configuration

To see the relevant list of CAS properties, please [review this guide](Configuration-Properties.html).

## Non-Browser MFA

The Duo Security module of CAS is able to also support [non-browser based multifactor authentication](https://duo.com/docs/authapi) requests.
In order to trigger this behavior, applications (i.e. `curl`, REST APIs, etc) need to specify a special
`Content-Type` to signal to CAS that the request is submitted from a non-web based environment.

In order to successfully complete the authentication flow, CAS must also be configured with a method 
of primary authentication that is able to support non-web based environments.

Here is an example using `curl` that attempts to authenticate into a service by first exercising
basic authentication while identifying the request content type as `application/cas`. It is assumed that the 
service below is configured in CAS with a special multifactor policy that forces the flow 
to pass through Duo Security as well.

```bash
curl --location --header "Content-Type: application/cas" https://apps.example.org/myapp -L -u casuser:Mellon
```
