#!/bin/bash
unzipfile() {
lines=$(find *.zip|wc -l)
if [ $lines -gt 0 ]; then
 for file in find *.zip
 do
   7z x -y ${file};
 done
fi
}

for i in "$@"
do
case $i in
    -svn=*|--svn=*)
    SVNREPO="${i#*=};$SVNREPO"
    shift # past argument=value
    ;;
    -nexus=*|--nexus=*)
    NEXUSREPO="${i#*=};$NEXUSREPO"
    shift # past argument=value
    ;;
    -cp=*|--copy=*)
    COPY="${i#*=};$COPY"
    shift # past argument=value
    ;;
    -jvm=*|--javargs=*)
    JAVARGS="${i#*=}"
    shift # past argument=value
    ;;
    -l=*|--argline=*)
    ARGLINE="${i#*=}"
    shift
    ;;
    -git=*|--gitrepo=*)
    GITREPO="${i#*=};$GITREPO"
    shift
    ;;
    -u=*|--user=*)
    USER="${i#*=}"
    shift
    ;;
    -p=*|--password=*)
    PASS="${i#*=}"
    shift
    ;;
    -gu=*|--git-user=*)
    GUSER="${i#*=}"
    shift
    ;;
    -gp=*|--git-password=*)
    GPASS="${i#*=}"
    shift
    ;;
    --default)
    DEFAULT=YES
    shift # past argument with no value
    ;;
    *)
          # unknown option
    ;;
esac
done
echo "SVNREPO = ${SVNREPO}"
echo "NEXUSREPO= ${NEXUSREPO}"
echo "COPY  = ${COPY}"
echo "JAVARGS = ${JAVARGS}"
echo "ARGLINE = ${ARGLINE}"
echo "GITREPO = ${GITREPO}"


USER=$(cat /etc/cred/user)
PASS=$(cat /etc/cred/pass)

MYFOLDER=$(pwd)
if [[ -n ${GITREPO} ]]; then
    echo "Git repo is not empty"
    IFS=';' read -ra GITREPOS <<< "${GITREPO}"
    for j in ${GITREPOS[@]}
    do
	  EXPECTED_FORMAT="HTTPS_GIT_REPO+BRANCH+GIT_FILES_MASK_1+FOLDER_1+GIT_FILES_MASK_2+FOLDER_2"
	  
      REPO=$(echo $(echo ${j}|cut -f 1 -d'+' -s)|sed "s/https:\/\//@/g")
      BRANCH=$(echo ${j}|cut -f 2 -d'+' -s)
      GITFOLDER1=$(echo ${j}|cut -f 3 -d'+' -s)
      FOLDER1=$(echo ${j}|cut -f 4 -d'+' -s)
      GITFOLDER2=$(echo ${j}|cut -f 5 -d'+' -s)
      FOLDER2=$(echo ${j}|cut -f 6 -d'+' -s)
      echo "N= $j"
      echo "REPO= ${REPO}"
      echo "BRANCH= ${BRANCH}"
      echo "GITFOLDER= ${GITFOLDER1}"
      echo "FOLDER= ${FOLDER1}"
      echo "GITFOLDER2= ${GITFOLDER2}"
      echo "FOLDER2= ${FOLDER2}"      
	  
	  if [[ -z ${BRANCH} ]] ; then
		>&2 echo "BRANCH is not specified in the GIT REPO: ${j}"
		>&2 echo "Expected format: ${EXPECTED_FORMAT}"
		exit 1
	  fi
	  
	  if [[ -z ${GITFOLDER1} ]] ; then
		>&2 echo "GITFOLDER1 is not specified in the GIT REPO: ${j}"
		>&2 echo "Expected format: ${EXPECTED_FORMAT}"
		exit 1
	  fi
	  
	  if [[ -z ${FOLDER1} ]] ; then
		>&2 echo "FOLDER1 is not specified in the GIT REPO: ${j}"
		>&2 echo "Expected format: ${EXPECTED_FORMAT}"
		exit 1
	  fi
	  
      git clone -b ${BRANCH} https://${USER}:${PASS}${REPO} prj/
	  
	  #check if git clone was completed
	  if [[ $? -gt 0 ]] ; then 
		>&2 echo "GIT REPO was not cloned: ${REPO}"
		exit 1
	  fi
	  
      mkdir ${FOLDER1} 2>/dev/null
      yes|cp -rp prj/${GITFOLDER1} ${FOLDER1}
      if [[ -n ${FOLDER2} ]]; then
        mkdir ${FOLDER2} 2>/dev/null
        yes|cp -rp prj/${GITFOLDER2} ${FOLDER2}
      fi
      rm -rf prj/
    done
fi
if [[ -n ${SVNREPO} ]]; then
    echo "Svn repo is not empty"
    IFS=';' read -ra SVNREPOS <<< "${SVNREPO}"
    for j in ${SVNREPOS[@]}
    do
	  EXPECTED_FORMAT="SVN_REPO+SVN_FILES_MASK_1+FOLDER_1+SVN_FILES_MASK_2+FOLDER_2"

      REPO=$(echo ${j}|cut -f 1 -d'+' -s)
      REPOFOLDER1=$(echo ${j}|cut -f 2 -d'+' -s)
      FOLDER1=$(echo ${j}|cut -f 3 -d'+' -s)
      REPOFOLDER2=$(echo ${j}|cut -f 4 -d'+' -s)
      FOLDER2=$(echo ${j}|cut -f 5 -d'+' -s)
      echo "N= $j"
      echo "REPO= ${REPO}"
      echo "RFOLDER1= ${REPOFOLDER1}"
      echo "FOLDER1= ${FOLDER1}"
      echo "RFOLDER2= ${REPOFOLDER2}"
      echo "FOLDER2= ${FOLDER2}"
      NDIR_CUT=$((`echo "${REPO}"|sed 's|[^/]||g;s|/$||'|wc -c` - 3))
      
	  if [[ -z ${REPOFOLDER1} ]] ; then
		>&2 echo "REPOFOLDER1 is not specified in the SVN REPO: ${j}"
		>&2 echo "Expected format: ${EXPECTED_FORMAT}"
		exit 2
	  fi
	  
	  if [[ -z ${FOLDER1} ]] ; then
		>&2 echo "FOLDER1 is not specified in the SVN REPO: ${j}"
		>&2 echo "Expected format: ${EXPECTED_FORMAT}"
		exit 2
	  fi
	  
      wget --user=${USER} --password=${PASS} -m -np -nH --cut-dirs=$NDIR_CUT --no-check-certificate ${REPO} -P svntmp
	  
	  #check if SVN data was downloaded
	  if [[ ! -d svntmp ]] ; then 
		>&2 echo "data was not downloaded from SVN: ${REPO}"
		>&2 echo "Expected format: ${EXPECTED_FORMAT}"
		exit 2
	  fi

      cd svntmp
      unzip \*.zip
      cd ..
      mkdir ${FOLDER1} 2>/dev/null
      yes|cp -rp svntmp/${REPOFOLDER1} ${FOLDER1}
      if [[ -n ${FOLDER2} ]]; then
        echo "folder2222"
        mkdir ${FOLDER2} 2>/dev/null
        yes|cp -rp  svntmp/${REPOFOLDER2} ${FOLDER2}
      fi
      rm -rf svntmp/
    done
fi
if [[ -n ${NEXUSREPO} ]]; then
    echo "Nexus repo is not empty"
    IFS=';' read -ra NEXUSREPOS <<< "${NEXUSREPO}"
    for j in ${NEXUSREPOS[@]}
    do
	  EXPECTED_FORMAT="NEXUS_REPO+NEXUS_FILES_MASK_1+FOLDER_1+NEXUS_FILES_MASK_2+FOLDER_2"

      REPO=$(echo ${j}|cut -f 1 -d'+' -s)
      REPOFOLDER1=$(echo ${j}|cut -f 2 -d'+' -s)
      FOLDER1=$(echo ${j}|cut -f 3 -d'+' -s)
      REPOFOLDER2=$(echo ${j}|cut -f 4 -d'+' -s)
      FOLDER2=$(echo ${j}|cut -f 5 -d'+' -s)
      echo "N= $j"
      echo "REPO= ${REPO}"
      echo "RFOLDER1= ${REPOFOLDER1}"
      echo "FOLDER1= ${FOLDER1}"
      echo "RFOLDER2= ${REPOFOLDER2}"
      echo "FOLDER2= ${FOLDER2}"
      NDIR_CUT=$((`echo "${REPO}"|sed 's|[^/]||g;s|/$||'|wc -c` - 3))

	  if [[ -z ${REPOFOLDER1} ]] ; then
		>&2 echo "REPOFOLDER1 is not specified in the NEXUS REPO: ${j}"
		>&2 echo "Expected format: ${EXPECTED_FORMAT}"
		exit 3
	  fi
	  
	  if [[ -z ${FOLDER1} ]] ; then
		>&2 echo "FOLDER1 is not specified in the NEXUS REPO: ${j}"
		>&2 echo "Expected format: ${EXPECTED_FORMAT}"
		exit 3
	  fi
	  
      #get timestamp from metadata.xml file to find out latest data in the REPO
      if [[ ${REPO} == *"LATEST"* ]]; then
        METADATAURL=${REPO%/*}/maven-metadata.xml
        echo ${METADATAURL}
        versionTimestamped=$(wget -q -O- --user=${USER} --password=${PASS} --no-check-certificate ${METADATAURL} | grep -m 1 \<value\> | sed -e 's/<value>\(.*\)<\/value>/\1/' | sed -e 's/ //g')
        echo ${versionTimestamped}
       	REPO=$(echo ${REPO}|sed "s/LATEST/${versionTimestamped}/")
        echo ${REPO} 
      fi
	  
      wget --user=${USER} --password=${PASS} -m -np -nH --cut-dirs=$NDIR_CUT --no-check-certificate ${REPO} -P nexustmp
	  
      #check if SVN data was downloaded
      if [[ ! -d nexustmp ]] ; then 
		>&2 echo "data was not downloaded from NEXUS: ${REPO}"
		exit 3
	  fi
	  
      cd nexustmp
      unzip \*.zip
      cd ..
      mkdir ${FOLDER1} 2>/dev/null
      yes|cp -rp nexustmp/${REPOFOLDER1} ${FOLDER1}
      if [[ -n ${FOLDER2} ]]; then
        echo "folder2222"
        mkdir ${FOLDER2} 2>/dev/null
        yes|cp -rp  nexustmp/${REPOFOLDER2} ${FOLDER2}
      fi
      rm -rf nexustmp/
    done
fi
if [[ -n ${COPY} ]]; then
    echo "Copy is not empty"
    IFS=';' read -ra COPIES <<< "${COPY}"
    for j in ${COPIES[@]}
    do
      FOLDER1=$(echo ${j}|cut -f 1 -d'+' -s)
      FOLDER2=$(echo ${j}|cut -f 2 -d'+' -s)
      echo "N= $j"
      echo "FOLDER1= ${FOLDER1}"
      echo "FOLDER2= ${FOLDER2}"
      yes|cp -rp ${FOLDER1} ${FOLDER2}
      cd ${FOLDER2}
      unzipfile
      cd ${MYFOLDER}
  done
fi


# enable trace http://www.faqs.org/docs/abs/HTML/options.html
# -x : Print each command to stdout before executing it (and expands command)
set -x

while getopts u:p:t:m:e: option
do
case "${option}"
in
u) echo atp.url=${OPTARG} >> test.properties;;
p) echo atp.project=${OPTARG} >> test.properties;;
t) echo atp.project.testplan=${OPTARG} >> test.properties;;
e) echo atp.project.er.name=${OPTARG} >> test.properties;;
m) echo atp.project.recipients=${OPTARG} >> test.properties;;
esac
done

export CLASSPATH="$CLASSPATH:./lib/*"
jybot --listener org.qubership.atp.adapter.robot.RamListener --outputdir result ./

mkdir /atp-adapter-robot/logs
mv /atp-adapter-robot/result/log.html /atp-adapter-robot/logs/log.html

set +x
