
# BuildVu Microservice Example API #

This document details the raw requests - for language specific wrappers / examples, see the [Usage section in the readme](README.md).

### Uploading ###
You have two options when Beginning a new conversion:

 - Option 1, Sending the file in the POST request:
    - Send a POST request containing the file to    convert (see POST request contents).    
    - The encoding type should be multipart/form-data and provide    a 'file' parameter containing the file data (content and filename).
    - Note: 'filename' is usually included automatically but may not be    depending on the tool generating the request.  
 - Option 2, Link the file through a URL:   
    - Send a URL in the post data with key "converstionUrl".
    - This server will attempt to download a file from that location.
    - The encoding does not have to be multipart/form-data.
    - Note: this option will be ignored if a file has been sent in the POST request.

**URL:** ```/buildvu```

**Method:** POST

**Params:**
* **Optional:** "filename": the name of the file (useful to send when passing the file via URL in case the name of the file cannot be parsed from the URL by the server).

**Example request:**

```
POST https://[URL]/buildvu
```

**POST request contents:**

```POST https://[URL]/buildvu
POST / HTTP/1.1
Content-Type: multipart/form-data; boundary=foo_boundary

--foo_boundary

    Content-Disposition: form-data; name="file", filename="example.pdf";
    Content-Type: application/pdf
    [Content of example.pdf]

foo_boundary--
```

**Success Response(s):**
* **Code** = 200
* **Content** = 

```
{
    "uuid": [String]
} 
```

**Error Response(s):**
* **Code** = 400
* **Content** = ```{error: "Missing file or URL"}```


* **Code** = 500
* **Content** = ```{error: "Missing file"}```

* **Code** = 500
* **Content** = ```{error: "Cannot get file data"}```

* **Code** = 500
* **Content** = ```{error: "Internal error"}```

___


### Status

To check the status of a conversion, send a GET request along with the UUID.

**URL:** ```/buildvu```

**Method:** GET

**Params:**
* **Required:** uuid = [String]
* **Optional:** none

**Example request:**

```GET https://[URL]/buildvu?uuid=[uuid]```


**Success Response(s):**
* **Code** = 200
* **Content** = 

```
{
    "state": [String], * Will be "queued", "downloading", "processing", "processed" or "error"
    "previewUrl": [String], * Only when state is processed
    "downloadUrl": [String], * Only when state is processed
    "error": [String] * Only when state is error
}
```
___


### Download

The converted output is available as a .zip archive at:

**URL:** ```/output/[uuid]/[filename].zip```

**Method:** GET

**Example request:**

```GET https://[URL]/output/[uuid]/[filename].zip```

___

### View

Results of the conversion are viewable at:

**URL:** ```/output/[uuid]/[filename]/index.html```

**Method:** GET

**Example request:**

```GET https://[URL]/output/[uuid]/[filename]/index.html```

___
