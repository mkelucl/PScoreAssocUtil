#!/bin/bash
#$ -S /bin/bash
#$ -o /home/xxxxxxxxx/tests/pscorebatch/test.out
#$ -e /home/xxxxxxxxx/tests/pscorebatch/test.err
#$ -l tmem=20G,h_vmem=20G
#$ -l h_rt=20:0:0
#$ -V
#$ -t 1-180

#echo "START1"
#pwd
#echo "START2"

#$ -N gfdgdf

PATH=~/bin:$PATH
PATH=/share/apps/R/bin:$PATH
java=/share/apps/jdk1.8.0_25/jre/bin/java

hostname
date


# create scracth directory
scracthDir='/scratch0/xxxxxxxxx'
if [[ ! -e $scracthDir ]]; then
    mkdir -p $scracthDir
fi

# DONT NEED TO DO THIS! -> it was just a rouge node
# the first 5 jobs are 'dummy' used as a buffer, in order to prevent the cluster failing the later jobs
#if [ $SGE_TASK_ID -lt 6 ];
#then
#	exit
#fi
#real_taskId=$(($SGE_TASK_ID-5));

real_taskId=$SGE_TASK_ID


## #$ -cwd

# commandLine paramters
homeBase='/home/xxxxxxxxx/tests/pscorebatch/'
#dirBase=$scracthDir$homeBase  #create everything under scratch
iterationDirBase=$scracthDir$'iterations/' # the iterations should go to the Scracth disk
taskDirBase='task'
jarFileBase='PScoreAssocUtil.jar'
resultsFolderBase=$homeBase$'results/'

# put all output files into a single folder for convenience
if [[ ! -e $resultsFolderBase ]]; then
    mkdir -p $resultsFolderBase
fi

# all task thread files will be put into a common 'iterations' directory
if [[ ! -e $iterationDirBase ]]; then
    mkdir -p $iterationDirBase
fi

# java parameters

# constant params that are always the same for all iterations
inputFile=' *inputvcf /home/xxxxxxxxx/datasets/1000genomes/ALL.chr16.phase3_shapeit2_mvncall_integrated_v5a.20130502.genotypes.vcf.gz'
regionParam=' *region 16 50731049 50766987'
variantsParam=' *variants /home/xxxxxxxxx/tests/pscorebatch/PSCORE_VAR.csv'
baseProbParam=' *baselineProb 0.00322'
seedParam=' *seed 1'
simoutputParam=' *simoutput myOutput.dat pheOutput.phe'
vepinputParam=' *vepinput /home/xxxxxxxxx/tests/pscorebatch/VEP_NOD2.csv'
weighstoutputParam=' *weightsoutput nod2.annot nod2weights.txt'
weightmapParam=' *weightmap /home/xxxxxxxxx/tests/pscorebatch/weigthmap.csv'
numsimsParam=' *numsims 200'
pscoreParam=' *pscorreassoc pscoreassoc'

# variable parameters
simSizeBase=' *simulate '   # 2000 2000  
weightFactorBase=' *weightfactor '
pvalueOutputDirBase=' *pvaloutput '$resultsFolderBase
resultsFileBase='logPValues_'



#  the '-t 1-100' parameter will cause this script to be executed a 100 times, from 1 to 100 ( includsive)
# with '$SGE_TASK_ID' being the 'iterator' having values from 1 to 100

# load in taskList file to find current task based on '$SGE_TASK_ID'
# Parse textfile line by line: http://stackoverflow.com/questions/10929453/bash-scripting-read-file-line-by-line
# split String: http://stackoverflow.com/questions/10586153/split-string-into-an-array-in-bash
n=1
while IFS='' read -r line || [[ -n $line ]]; do # goes through file line by line
   # echo "Text read from file: $line"
	
	if [ $n -eq $real_taskId ];
	then
		IFS=$'\t' read -a array <<< "$line" # split line by tab
		currentWeight=${array[0]}
		currentSimSize=${array[1]}
		
		#echo  "currentWeight from file: $currentWeight"
		#echo  "currentSimSize from file: $currentSimSize"
	fi
	
	n=$(( $n + 1 ))

done < $homeBase$"taskList.csv"

# make directories: http://stackoverflow.com/questions/793858/how-to-mkdir-only-if-a-dir-does-not-already-exist

# create working directory for iteration
taskOutputName=$(printf "%03d\n" $real_taskId)
taskDir=$iterationDirBase$taskDirBase$taskOutputName

# if the task results already exists: http://stackoverflow.com/questions/59838/check-if-a-directory-exists-in-a-shell-script
# abandon job, as it already has been done
if [[ -e $resultsFolderBase$resultsFileBase$taskOutputName ]]; then # check if the results file already exists  ... use this check task failed after creating task directory ( IE resubmitting a job a few days later)
# if [[ -e $taskDir ]]; then   # check if task directory exists... use this check if task is 're submitted' while one copy is already running
echo "task results for $real_taskId already exists: abort"
    exit
fi

mkdir -p $taskDir # -p makes it if it wasnt already made

# copy .jar into this ( as temporary files will be written into same directory as .jar is)
#taskJarLocation=$taskDir$'/'$jarFileBase 
# cp $scracthDir$jarFileBase $taskJarLocation  # no need to copy the jarfile

taskJarLocation=$homeBase$jarFileBase 


# create execute command
date
echo "executing $real_taskId"
cd $taskDir # on Unix Java does not know where the .jar file is, it always treats the current working dir as the dir it will work from
commands=$taskJarLocation$inputFile$simSizeBase$' '$currentSimSize$' '$currentSimSize$regionParam$variantsParam$baseProbParam$seedParam$simoutputParam$vepinputParam$weighstoutputParam$numsimsParam$pscoreParam$pvalueOutputDirBase$resultsFileBase$taskOutputName$weightmapParam$weightFactorBase$currentWeight
echo "commands: $commands"
$java -Xms4g -Xmx4g -jar $commands

# once current job is finished, delete folder
rm -rf $taskDir
date






# taskJarLocation='/home/xxxxxxxxx/tests/pscorebatch/iterations/task010/ClusterTest.jar'
# $java -Xms4g -Xmx4g -jar ClusterTest.jar
# $java -Xms4g -Xmx4g -jar $taskJarLocation

# /home/xxxxxxxxx/tests/pscorebatch/iterations/task010/ClusterTest.jar
# /home/xxxxxxxxx/tests/pscorebatch/iterations/task010
