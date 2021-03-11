# Quarkus Splunk extension

<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-2-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

A quarkus Extension to send logs to a Splunk Http Event Collector (HEC).

To get started, add the dependency:

```xml
<dependency>
    <groupId>io.quarkiverse.logging.splunk</groupId>
    <artifactId>quarkus-logging-splunk</artifactId>
</dependency>
```

For more details, check the complete [documentation](https://quarkiverse.github.io/quarkiverse-docs/quarkus-logging-splunk).

This extension is based on the [official Splunk's HEC client](https://github.com/splunk/splunk-library-javalogging).
But, it defines its own [Java Logging Handler](https://docs.oracle.com/en/java/javase/11/docs/api/java.logging/java/util/logging/Handler.html)
rather than using the [official one](https://github.com/splunk/splunk-library-javalogging/blob/1.8.0/src/main/java/com/splunk/logging/HttpEventCollectorLoggingHandler.java),
the reason for that is, it was not compatible with JBoss logger implementation used in Quarkus and, as such,
prevent to use the ability to record steps at build time.

## Contributors ✨

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tr>
    <td align="center"><a href="https://github.com/vietk"><img src="https://avatars.githubusercontent.com/u/1568850?v=4?s=100" width="100px;" alt=""/><br /><sub><b>vietk</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-splunk/commits?author=vietk" title="Code">💻</a> <a href="#maintenance-vietk" title="Maintenance">🚧</a></td>
    <td align="center"><a href="https://github.com/rquinio1A"><img src="https://avatars.githubusercontent.com/u/58322910?v=4?s=100" width="100px;" alt=""/><br /><sub><b>rquinio1A</b></sub></a><br /><a href="https://github.com/quarkiverse/quarkus-splunk/commits?author=rquinio1A" title="Code">💻</a> <a href="#maintenance-rquinio1A" title="Maintenance">🚧</a></td>
  </tr>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification. Contributions of any kind welcome!
