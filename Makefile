

############################################################################
# Development
############################################################################
.PHONY: default run build

default:
	@echo Default is blank

run:
	./gradlew


FAT_JAR=build/libs/rocket-fat.jar

build:
	./gradlew shadowJar

exec: ${FAT_JAR}
	java -jar ${FAT_JAR}

JAVA_OPTS=-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory
run: build
	#java ${JAVA_OPTS} -cp ${FAT_JAR} com.lfmunoz.AppKt start
	#java -cp ${FAT_JAR} com.lfmunoz.AppKt run eco.usp.harness.MainVerticle --launcher-class=eco.usp.harness.PerfLauncher -conf src/main/resources/application-config.json

server: build
	java -cp ${FAT_JAR} com.lfmunoz.AppKt run com.lfmunoz.server.Main --launcher-class=com.lfmunoz.App -conf ./src/main/resources/conf/config.json

client: build
	java -cp ${FAT_JAR} com.lfmunoz.AppKt run com.lfmunoz.client.Main --launcher-class=com.lfmunoz.App -conf ./src/main/resources/conf/config.json

############################################################################
# Production
############################################################################
GIT_HASH=$(shell git log --pretty=format:'%h' -n 1)
SERVER=c12-24
CLIENT=perf

upload:
	./gradlew shadowJar
	scp build/libs/rocket-fat.jar ${SERVER}:~/rocket/rocket${GIT_HASH}.jar
	scp build/libs/rocket-fat.jar ${CLIENT}:~/rocket/rocket${GIT_HASH}.jar
	ssh c12-24 "cd; cd rocket; echo RCOMMIT=${GIT_HASH} >> Makefile"
	ssh perf "cd; cd rocket; echo RCOMMIT=${GIT_HASH} >> Makefile"


config:
	ssh c12-24 "mkdir -p rocket"
	ssh perf "mkdir -p rocket"
	scp src/main/resources/conf/config.json ${SERVER}:~/rocket/rconfig.json
	scp src/main/resources/conf/config.json ${CLIENT}:~/rocket/rconfig.json
	scp src/main/resources/logback.xml ${SERVER}:~/rocket/logback.xml
	scp src/main/resources/logback.xml ${CLIENT}:~/rocket/logback.xml
	scp src/main/resources/Makefile.mk ${SERVER}:~/rocket/Makefile
	scp src/main/resources/Makefile.mk ${CLIENT}:~/rocket/Makefile

