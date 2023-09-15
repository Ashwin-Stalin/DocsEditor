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
 
### GET /docs/{docid}/shared-users
	To get all the users and their permissions of shared document owned by user
 
### GET /docs/{docid}/shared-users/{userid}
	To get a user and their permission of shared document owned by user

### POST /docs
	To create a document
 	Parameter: {doc_name}

 ### POST /docs/{docid}/shared-users
 	To share a document with users
  	Parameter: {username} {permission}

### PUT /docs/{docid}
	To update a content of a document

### PUT /docs/{docid}/shared-users/{userid}
	To update a permission of a shared document
 	Parameter: {permission}

### PUT /docs/{docid}/versions/{versionid}
	To update a current version of a document
 	Parameter: {versionid}

### DELETE /docs/{docid}
	To delete a document which is owned
 
### DELETE /docs/{docid}/shared-users
	To delete all the shared users for a document

### DELETE /docs/{docid}/shared-users/{userid}
	To delete a shared user for a document



  
