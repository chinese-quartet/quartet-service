# Quartet Service

The Quartet Project provides publicly accessible multi-omics reference materials and practical tools to enhance the reproducibility and reliability of multi-omics results. Well-characterized multiomics reference materials and quality control metrics pertinent to precision medicine study purposes can be used to measure and mitigate technical variation, enabling more accurate cross-batch and cross-omics data integration in increasingly large-scale and longitudinal studies such as the International Human Phenome Project.

[![Latest Release](https://img.shields.io/github/v/release/chinese-quartet/quartet-service?sort=semver)](https://github.com/chinese-quartet/quartet-service/releases)
[![Docker Image](https://github.com/chinese-quartet/quartet-service/actions/workflows/publish.yaml/badge.svg)](https://github.com/chinese-quartet/quartet-service/actions/workflows/publish.yaml)
[![License](https://img.shields.io/github/license/chinese-quartet/quartet-service)](https://github.com/chinese-quartet/quartet-service/blob/master/LICENSE.md)

## For user
### Configuration

#### Configuration File Mode
```clojure
{:port 8089
 ;; when :nrepl-port is set the application starts the nREPL server on load
 :nrepl-port 7000
 ;; :database-url "postgresql://localhost:5432/quartet_service_dev?user=postgres&password=password"
 :database-url "jdbc:sqlite:./quartet-service/quartet_service_dev.db"
 :workdir "./quartet-service"
 :plugin-rootdir "./quartet-service/"
 :fs-services [{:fs-service             "minio"
                :fs-endpoint            "http://localhost:9000"
                :fs-access-key          "XXXXXXXXXXXX"
                :fs-secret-key          "XXXXXXXXXXXX"
                :fs-rootdir             "/data/minio"}
               {:fs-service             "oss"
                :fs-endpoint            "http://oss-cn-shanghai.aliyuncs.com"
                :fs-access-key          "XXXXXXXXXXXX"
                :fs-secret-key          "XXXXXXXXXXXX"
                :fs-rootdir             ""}]
 :default-fs-service "minio"
 :plugins {}
 :enable-cors false
 :cors-origins nil}
```

#### Environment Mode

```bash
# Port
export PORT=3000
# NREPL Port
export NREPL_PORT=7000

# Database(Support PostgreSQL, H2, SQLite)
## PostgreSQL
export DATABASE_URL="postgresql://localhost:5432/quartet_service_dev?user=postgres&password=password"

## H2
export DATABASE_URL="jdbc:h2:./quartet_service_dev.db"

## SQLite
export DATABASE_URL="jdbc:sqlite:./quartet_service_dev.db"

# Quartet Service Working Directory
export WORKDIR=./

# Quartet Service Plugin Path
export PLUGIN_ROOTDIR=./
```

### Using docker image

1. Go to the Packages page in the `https://github.com/clinico-omics/quartet-service`
2. Choose a version
3. Pull your expected package with the following command

```
docker pull ghcr.io/clinico-omics/quartet-service:v0.3.2-9e3daa48
```

### Download jar package

```
# TODO
```

## For Developer
### Prerequisites

1. You will need [Leiningen][1] 2.0 or above installed.
2. Clone the `quartet-service` repo
   
   ```
   git clone https://github.com/clinico-omics/quartet-service.git
   cd quartet-service
   ```

3. Prepare a configuration file and save as `dev-config.edn` into the `quartet-service` directory
   
   ```
   ;; WARNING
   ;; The dev-config.edn file is used for local environment variables, such as database credentials.
   ;; This file is listed in .gitignore and will be excluded from version control by Git.

   {:port 3000
    ;; when :nrepl-port is set the application starts the nREPL server on load
    :nrepl-port 7000
    :database-url "postgresql://localhost:5432/quartet-service_dev?user=postgres&password=password"
    :workdir "~/Downloads/quartet-service"
    :plugin-rootdir "~/Downloads/quartet-service/"
    :fs-services [{:fs-service             "minio"
                    :fs-endpoint            "http://10.157.72.56:9000"
                    :fs-access-key          "test"
                    :fs-secret-key          "4gmPNjG5JKRXXXXXuxTqO"
                    :fs-rootdir             "/data/minio"}
                  {:fs-service             "oss"
                    :fs-endpoint            "http://oss-cn-shanghai.aliyuncs.com"
                    :fs-access-key          "LTAI4Fi5MEXXXXXzhjEEF43a"
                    :fs-secret-key          "hQhPB8tRFloXXXXXXhKv1GOLdwFVLgt"
                    :fs-rootdir             ""}]
    :default-fs-service "minio"
    :plugins {}}
   ```

4. [Install PostgreSQL and create the `quartet-service_dev` database for development](#build-dev-db)

### Install Dependencies

```bash
lein deps
```

### Running

To start a web server for the application, run:

```bash
lein run 
```

### How to reload application without the need to restart the REPL itself

```
(require '[user :as u])
(u/restart)
```

### How to get a quartet-service plugin?

```
# TODO
```

### How to develop a quartet-service plugin?

#### Quartet Service Plugin Manifest
```yaml
info:
  name: Quartet DNA-Seq Report
  version: v1.0.1
  description: Parse the results of the quartet-dna-qc app and generate the report.
  category: Tool
  home: https://github.com/clinico-omics/quartet-service-plugins
  source: PGx
  short_name: quartet-dnaseq-report
  icons:
    - src: ""
      type: image/png
      sizes: 192x192
  author: Jingcheng Yang
plugin:
  name: quartet-dnaseq-report
  display-name: Quartet DNA-Seq Report
  lazy-load: false
init:
  # Unpack any files to the specified directory, such as ENV/CONFIG/DATA directory.
  # You can write the unpack-env step more than once.
  - step: unpack-env
    # envname means the name of the file/directory in the resources directory, the file extension can be "tar.gz", "tgz" or "". When the envtype is environment, you need to keep envname same with the plugin name.
    envname: quartet-dnaseq-report
    # envtype can be the one of 'environment', 'configuration', 'data'
    envtype: environment
    # post-unpack-cmd can use template variables, such as {{ ENV_DEST_DIR }}, {{ CONFIG_DIR }}, {{ DATA_DIR }}
    post-unpack-cmd: 'FIXME: you can write any bash command'
  - step: load-namespace
    namespace: quartet-service.plugins.quartet-dnaseq-report
  - step: register-plugin
    entrypoint: quartet-service.plugins.quartet-dnaseq-report/metadata
  - step: init-event
    entrypoint: quartet-service.plugins.quartet-dnaseq-report/events-init
```

#### How to get the plugin context?

If you use `make-plugin-metadata` and `make-routes` to generate routes, then you can get the following variables from the handler's argument.

```clojure
{
  ;; All fields which you defined by `body-schema`, `query-schema`, `path-schema`
  :owner          "current user, maybe email or username."
  :workdir        "working directory, you can use it as the output directory."
  :uuid           "uuid is the part of workdir and maybe you need to use it to create a task."
  :plugin-context {:plugin-name "plugin-name"
                   :plugin-version "plugin-version"
                   :plugin-info "The content of tservice-plugin.yml"
                   :data-dir "The data directory of the specified plugin, you can generate the data into the directory, all data can be shared by generated tasks of the plugin."
                   :env-dir "The env directory of the specified plugin."
                   :jar-path "The jar path of the specified plugin."
                   :config-dir "The config directory of the specified plugin. When you need to access static files from the plugin, you can located these files into resources directory. Quartet service will copy all these files into config directory if you define a specified unpack-env step in tservice-plugin.yml."}
}
```

### [How to auto-generate docs?](https://github-wiki-see.page/m/weavejester/codox/wiki/Deploying-to-GitHub-Pages)

1. Commit all code modifications
2. Give a tag for the latest commit
3. Build your documentation with `lein codox`
4. To publish your docs to Github Pages, run the following commands
   
   ```
   cd docs
   git add .
   git commit -am "Update docs."
   git push -u origin gh-pages
   cd ..
   ```

### How to build docker image?

```
make build-docker
```

### <span id="build-dev-db">How to create a database for development?</span>

```
make dev-db
```

## License

Copyright © 2015-2021 Eclipse Public License 2.0

[1]: https://github.com/technomancy/leiningen