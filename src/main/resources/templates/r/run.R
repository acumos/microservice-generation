#matching acumos R package version 0.2-8
ready_path<-function(path){
  if (dir.exists(path)) {
    given_dir <- path
    if(!(file.exists(file.path(given_dir, "component.bin"))
    &&file.exists(file.path(given_dir, "component.proto"))
    &&file.exists(file.path(given_dir, "meta.json")))){
      ready_dir <- tempfile("acumos-runtime")
      if (!all(dir.create(ready_dir))) stop("unable to create temporary directory in `",ready_dir,"' to copy and rename the components")
      binFile<-tools::list_files_with_exts(given_dir, exts = 'bin', all.files = FALSE, full.names = TRUE)
      if(length(binFile)>0){
        fileName<-tools::file_path_sans_ext(binFile)
        file.link(binFile, file.path(ready_dir, "component.bin"))
      }else{
        stop('no bin file')
      }
      if(file.exists(paste0(fileName,'.proto'))){
        protoFile<-paste0(fileName,'.proto')
        file.link(protoFile, file.path(ready_dir, "component.proto"))
      }else{
        stop('no proto file sharing the bin file name')
      }
      if(file.exists(paste0(fileName,'.json'))){
        jsonFile<-paste0(fileName,'.json')
        file.link(jsonFile, file.path(ready_dir, "meta.json"))
      }else{
        stop('no json file sharing the bin and proto files name')
      }
      target<-ready_dir
    }else{
      target<-given_dir
    }
  }else{
    target<-path
  }
return(target)
}
acumos:::run(file=ready_path("."), runtime=file.path(".","runtime.json"))