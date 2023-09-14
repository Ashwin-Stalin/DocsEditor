# DocsEditor
TomcatServer, PostgreSQL Database

## Usage

### GET /login
	Returning API Key
	Parameter: {uname=?}{pass=?}

### POST /register
	To create User
	Parameter: {uname=?}{pass=?}{cpass=?}

### GET /docs
	To get all documents which is created by user and shared by other users

### GET /docs/{docid}
	To get specific document by id which is created by user or shared by other users

### GET /docs/{docid}/versions
	To get all versions of a document

### GET /docs/{docid}/versions/{versionid}
	To get content of specific version of a document
