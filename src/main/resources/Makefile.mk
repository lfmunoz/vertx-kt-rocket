


RCOMMIT=9a0c581
ROCKET_FILE=rocket${RCOMMIT}.jar
ROCKET_LOG=rocket${RCOMMIT}.log

run:
	nohup java -Xms20G -Xmx32G -Djava.net.preferIPv4Stack=true -Dlogback.configurationFile=./logback.xml -cp ${ROCKET_FILE}  com.lfmunoz.AppKt run com.lfmunoz.server.Main --launcher-class=com.lfmunoz.App --conf rconfig.json > ${ROCKET_LOG} 2>&1 &
	echo $!


tail:
	tail -1000f ./${ROCKET_LOG}



