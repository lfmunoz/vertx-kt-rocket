

.PHONY: default run build

default:
	@echo Default is blank

run:
	./gradlew


FAT_JAR=build/libs/rocket-fat.jar

build:
	./gradlew shadowJar

${FAT_JAR}:
	./gradlew shadowJar

exec: ${FAT_JAR}
	java -jar ${FAT_JAR}


client: build
	java -cp ${FAT_JAR} com.lfmunoz.client.MainKt

server: build
	java -cp ${FAT_JAR} com.lfmunoz.server.MainKt

both:
	java -cp ${FAT_JAR} com.lfmunoz.server.MainKt &
	java -jar ${FAT_JAR} com.lfmunoz.client.MainKt &


GIT_HASH=$(shell git log --pretty=format:'%h' -n 1)
SERVER=c12-24:~
CLIENT=c12-28:~

upload:
	./gradlew shadowJar
	scp build/libs/rocket-fat.jar ${SERVER}/rocket${GIT_HASH}.jar
	scp build/libs/rocket-fat.jar ${CLIENT}/rocket${GIT_HASH}.jar
	ssh perf "cd; echo INDEX=${GIT_HASH} >> Makefile"


