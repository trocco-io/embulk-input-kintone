# kintone input plugin for Embulk
[![Build Status](https://travis-ci.org/trocco-io/embulk-input-kintone.svg?branch=master)](https://travis-ci.org/trocco-io/embulk-input-kintone)

## Overview
kintone input plugin for Embulk loads app records from kintone.
embulk 0.9 is only supported due to the dependency of kintone-java-sdk 0.4.0, which requires java 8

This plugin uses [cursor API](https://developer.kintone.io/hc/en-us/articles/360000280322). See the limitation on this page.
e.g. limit, offset are not supported.

* **Plugin type**: input
* **Resume supported**: no
* **Cleanup supported**: no
* **Guess supported**: no

## Road Map
- [ ] Guess
- [ ] field name mapping
- [ ] handle certification fot authentication

## Configuration

- **domain**: Kintone domain(FQDN) e.g. devfoo.cybozu.com (string, required)
- **username**: Kintone username (string, optional)
- **password**: Kintone password (string, optional)
- **token**: Kintone app token. Username and password or token must be configured. If all of them are provided, this plugin uses username and password (string, optional)
- **app_id**: Kintone app id (integer, required)
- **query**:  Kintone query to retrieve records. If the query is omitted, all records are retrieved. The query syntax follows [official documentation: Query Operators and Functions](https://developer.kintone.io/hc/en-us/articles/360019245194) (string, optional)
- **basic_auth_username**:  Kintone basic auth username Please see Kintone basic auth [here](https://jp.cybozu.help/general/en/admin/list_security/list_ip_basic/basic_auth.html) (string, optional)
- **basic_auth_password**:  Kintone basic auth password (string, optional)
- **guest_space_id**: Kintone app belongs to guest space, guest space id is required. (integer, optional)
- **expand_subtable**: Expand subtabble (boolean, default: `false`)
- **fields**: If fields is empty, include all available columns (required)
  - **name** the field code of Kintone app record will be retrieved.
  - **type** Column values are converted to this embulk type. Available values options are: boolean, long, double, string, json, timestamp) Kintone `SUBTABLE` type is loaded as json text.
  - **format** Format of the timestamp if type is timestamp. The format for kintone DATETIME is `%Y-%m-%dT%H:%M:%S%z`.

kintone API has the limitation, therefore this plugin also faces it. See [official documentation](https://developer.kintone.io/hc/en-us/articles/212495188/)

## Example

```yaml
in:
  type: kintone
  domain: example.cybozu.com
  username: user
  password: password
  app_id: 1
  fields:
    - {name: $id, type: long}
    - {name: $revision, type: long}
    - {name: foo, type: string}
    - {name: bar, type: long}
    - {name: baz, type: double}
```

Query example

```yaml
in:
  type: kintone
  domain: example.cybozu.com
  username: user
  password: password
  app_id: 1
  query: Time > 10:00 and Time < 19:00 and Created_datatime = TODAY() order by $id asc
  fields:
    - {name: $id, type: long}
    - {name: $revision, type: long}
    - {name: Time, type: string}
    - {name: Created_datatime, type: string}
    - {name: foo, type: string}
    - {name: datetime, type: timestamp, format: '%Y-%m-%dT%H:%M:%S%z'}
```

## Build

```
$ ./gradlew gem  # -t to watch change of files and rebuild continuously
```

## Development
```
$ ./gradew build
$ ./gradew test
```
