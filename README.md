# TORGI

[![Waffle.io - Columns and their card count](https://badge.waffle.io/sofwerx/TORGI.svg?columns=all)](https://waffle.io/sofwerx/TORGI)
[![Build Status](https://travis-ci.org/sofwerx/TORGI.svg?branch=master)](https://travis-ci.org/sofwerx/TORGI)

## Tactical Observation of RF &amp; GNSS Interference

Collect, store, and display observations and analysis of GPS "fix" availability and quality.

Android 7.0 or higher required; 8.0 recommended. Google Pixel 2 (or Pixel 2 XL) provides Automatic Gain Control data.

#### [Compiled APK (Android app installer)](https://github.com/sofwerx/TORGI/releases/)

GNSS observation points are stored in a [GeoPackage](http://www.geopackage.org/) file with
satellite and receiver clock details using the (proposed)
[Related Tables Extension](https://github.com/opengeospatial/geopackage-related-tables/wiki/Getting-Started) and the [NGA GeoPackage Android SDK](https://ngageoint.github.io/geopackage-android/).

Observation points and associated location fix data are stored as a feature layer within a standard (v1.2) GeoPackage. The Data Model diagram below shows Satellite measurement data and receiver (hardware) clock information tables and their relationships, as currently implemented in the [TORGI app](https://github.com/sofwerx/TORGI/releases/).

The Related Tables Extension is a community standard extension to the GeoPackage map database, moving toward full adoption by [OGC (Open Geospatial Consortium)](http://www.opengeospatial.org/).

## Open Architecture / Open Standards

![Open Architecture / Open Standards data + communication model](docs/AMOpenArchitecture.png)

([View / Edit diagram in browser](https://www.draw.io/?url=https%3A%2F%2Fgithub.com%2Fsofwerx%2FTORGI%2Fraw%2Fmaster%2Fdocs%2FAMOpenArchitecture.png%3Ft%3D0))

### GeoPackage / SQLite Database ERD ###
<img src="https://github.com/sofwerx/TORGI/blob/master/docs/TORGI-GPKG-ERD.png?raw=true" />

## DevOps

`docker-compose up`

Android .apk will be build inside the Docker container: (`torgi/build/outputs/apk/{debug|release}`)

See Dockerfile for build process. Requires Android build tools and [NGA GeoPackage Android SDK](https://ngageoint.github.io/geopackage-android/).

Waffle.io - project tracking:
https://waffle.io/sofwerx/TORGI

Travis CI - automated continuous integration: https://travis-ci.org/sofwerx/TORGI

## SOFWERX
[SOFWERX](https://www.sofwerx.org) was created under a Partnership Intermediary Agreement between [Doolittle Institute](www.defensewerx.org) and [U.S. Special Operations Command](https://www.socom.mil) in Tampa, Florida. We are a platform designed and operated to help solve challenging warfighter problems through increased collaboration and innovation. Our charter is twofold, to maintain a platform to accelerate delivery of innovative capabilities to USSOCOM, and to facilitate capability refinement through exploration, experimentation, and assessment of promising technology.

[![Website](https://img.shields.io/badge/web-www.sofwerx.org-orange.svg)](https://www.sofwerx.org)
[![Twitter Follow](https://img.shields.io/twitter/follow/espadrine.svg?style=social&label=Follow)](https://twitter.com/sofwerx)
