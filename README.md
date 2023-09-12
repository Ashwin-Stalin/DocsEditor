# DocsEditor
TomcatServer, PostgreSQL Database

## Usage

GET /login<br/>
  Return API Key<br/>
	Parameter: {uname=?}{pass=?}

POST /register<br/>
  Creates User<br/>
	Parameter: {uname=?}{pass=?}{cpass=?}

GET /docs<br/>
	To get all documents which is created by user<br/>
	Parameter : {sharedwithme = false | null }

GET /docs<br/>
	To get specific document by id which is created by user<br/>
	Parameter : {docid=?} {sharedwithme = false | null }

GET /docs <br/>
	To get all documents which is shared to user<br/>
	Parameter : {sharedwithme = true}

GET /docs<br/>
	To get specific document by id which is shared to user<br/>
	Parameter : {docid=?} {sharedwithme = true}

POST /docs<br/>
	To create new document<br/>
	Parameter : {name=?} 

PUT /docs<br/>
	To update a specific document by id<br/>
	Parameter : {docid=?}

DELETE /docs<br/>
	To delete a specific document by id<br/>
	Parameter : {docid=?}

 GET /share<br/>
	To get all documents that has been shared by user<br/>

GET /share<br/>
	To get all users for a specific shared document by id <br/>
	Parameter : {docid=?}

POST /share<br/>
	To share owned document with user with permission<br/>
	Parameter : {docid=?}{username=?}{permission= View-Only | All }

PUT /share<br/>
	To update permission for shared document by id<br/>
	Parameter : {docid=?}{userid=?}{permission= View-Only | All }

DELETE /share<br/>
	To delete a specific shared document by id<br/>
	Parameter : {docid=?}

DELETE /share<br/>
	To delete a specific shared document by id and receiveduserid<br/>
	Parameter : {docid=?}{userid=?}

GET /versions<br/>
	To get all versions for docid and it's current version<br/>
	Parameter : {docid=?}

GET /versions<br/>
	To get content of particular document in a specific version<br/>
	Parameter : {docid=?}{versionid=?}

PUT /versions<br/>
	To update the current version to specific version<br/>
	Parameter : {docid=?}{versionid=?}
