# etcd config spring boot
## Introduction
随着kubernetes的快速发展，etcd也越来越火。很多公司运维着etcd集群，如果采用etcd作为配置中心，springboot集成不是很方便。为方便springboot集成etcd配置中心功能，开发了这个项目。

## Features
* 支持通过docker快速部署一个etcd配置中心服务，方便演示
* 提供sample，通过springboot starter方式可快速集成
* 支持properties和yml两种配置格式，支持动态刷新，配置优先级等
* 提供配置更新的回调通知，可感知配置变化

## Quick Start

[配置中心部署](etcd-config-server.md) <br>
[springboot集成](etcd-config-starter.md)<br>
[sample](etcd-config-spring-boot-sample)<br>