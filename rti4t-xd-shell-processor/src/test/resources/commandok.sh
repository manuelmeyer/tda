#
# echo stdin back and exit with a status of 0
#
while read k
do
	echo $k
done
env
exit 0