#CMS Gateway Java API
=====================

##Build
In order to build library jar, use following command in 'client' directory:

```shell
./gradlew --daemon clean build
```

If build is successful, jar file can be found in 'client/build/libs' directory.

In order to build library jar, install it to local maven repo, calculate SHA and MD5 on artifact use following command in 'client' directory:

```shell
./gradlew --daemon clean build install generateCheckSums
```

If build is successful, jar and MD5 and SHA checksum files can be found in 'client/build/libs' directory. Generated pom file with checksum are in client/build/poms directory.