

.PHONY: default run

default:
	@echo Default is blank

run:
	./gradlew


FAT_JAR=build/libs/rocket-fat.jar

build: ${FAT_JAR}
	./gradlew shadowJar

exec: build
	java -jar ${FAT_JAR}

