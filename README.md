# DocsEditor
TomcatServer, PostgreSQL Database

### Usage

#### GET /login
	Returning API Key
	Parameter: {uname=?}{pass=?}

### POST /register
	To create User
	Parameter: {uname=?}{pass=?}{cpass=?}

### GET /docs
	To get all documents which is created by user
	Parameter : {sharedwithme = false | null }

### GET /docs
	To get specific document by id which is created by user
	Parameter : {docid=?} {sharedwithme = false | null }

### GET /docs 
	To get all documents which is shared to user
	Parameter : {sharedwithme = true}

### GET /docs
	To get specific document by id which is shared to user
	Parameter : {docid=?} {sharedwithme = true}

### POST /docs
	To create new document
	Parameter : {name=?} 

### PUT /docs
	To update a specific document by id
	Parameter : {docid=?}

### DELETE /docs
	To delete a specific document by id
	Parameter : {docid=?}

 ### GET /share
	To get all documents that has been shared by user

### GET /share
	To get all users for a specific shared document by id 
	Parameter : {docid=?}

### POST /share
	To share owned document with user with permission
	Parameter : {docid=?}{username=?}{permission= View-Only | All }

### PUT /share
	To update permission for shared document by id
	Parameter : {docid=?}{userid=?}{permission= View-Only | All }

### DELETE /share
	To delete a specific shared document by id
	Parameter : {docid=?}

### DELETE /share
	To delete a specific shared document by id and receiveduserid
	Parameter : {docid=?}{userid=?}

### GET /versions
	To get all versions for docid and it's current version
	Parameter : {docid=?}

### GET /versions
	To get content of particular document in a specific version
	Parameter : {docid=?}{versionid=?}

### PUT /versions
	To update the current version to specific version
	Parameter : {docid=?}{versionid=?}
