README for uploader_example
===========================

## Introduction

This application is to demonstrate how to connect ng-flow (the file uploader) to
a JHipster-based application.

This example will implement a very simple file uploader resource/service to handle
the required functions of the ng-flow client library.  It will save all uploaded
chunks/files into the src/main/webapp/assets directory.

## Description (or what you need to know)

The basic idea is that the ng-flow client will get a new flow identifier at the
start of a flow.  We have a function to handle this that returns a random UUID.

Next the client will test if a chunk is already uploaded.  We return 502 if it hasn't.
This is some of the weirdness with the HTTP codes that I haven't quite figured out
yet.  I think this could be handled better.

Assuming the test returns the 502 then the client will do a MultiPart POST with
the data for the next chunk.  We take this and save off the chunk.

We track which chunks we have received and which ones we haven't.

When the last chunk arrives we will merge them back into the original file and
delete the chunks.

### Client Setup

Beyond the bower installation, you will also need to include the 'flow' service
in the Angular app.js file and add the addition flowFileProvider configuration section.

We are configured to only allow one file upload at a time.

We also specify the location of the unqiue identifier function.

### Client Usage

See the app/uploader client code for how to use the ng-flow library.  Basically
just add the flow directives at the top.

You can also do fancy things like track the upload progress with a progress bar
and display info about the upload after it is done.

### Upload Domain Object

A simple object that tracks the original file name from the client along with
expected chunks and file size.

Several things to note:
 - LocalDate should really be LocalDateTime but there is no JHipster support
 - Manually added List<Boolean> because there is no JHipster support
 - ID is a String but we always put a UUID.toString there
 
### UploadServiceImpl

The most basic I could think of was to save locally.  A better implementation
would put the file somewhere that they could be retrieved again.  And probably
not store them in the project path.

### UploadResource

Basically just obeys the requirements for the flowJS library that underpins the
ng-flow library.