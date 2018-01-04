# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [2.5.0] - unknown
Version compatible with ES 6.1.x and above
### Added
- Compatibility with ES 6.1.x (**not compatible anymore with 5.6.x, nor 6.0.0**) 

## [2.4.0] - 2017-12-21
Version only compatible with ES 6.0
### Modified
- Compatibility ES 6.0.0 (**not compatible anymore with 5.6.x, nor 6.1.x**)

## [2.3.0] - 2017-12-21
Version only compatible with ES 5.x
### Added
- Specify indexName through annotation (via annotation ElasticsearchDocument, indexName property) 
### Modified
- Compatibility ES 5.x (**not compatible with 6.x**)
- ElasticSearchEntitySyncInterceptor.onSave throws a NullPointerException if si rubiks.elasticsearch.sync.enabled = false in application.yml
