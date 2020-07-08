cd ..
rm runtime/dca-*.jar 2>/dev/null
mvn clean package

if [ $? != 0 ]
then
	echo built failed
	exit 1
fi
cp target/dca-*.jar runtime
cd runtime
echo java -jar dca-*.jar > run-dca.sh
chmod +x run-dca.sh
