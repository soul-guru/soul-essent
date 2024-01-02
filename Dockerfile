FROM ubuntu:focal


WORKDIR /app

COPY build/libs/i2-essent-all.jar /app/main.jar
COPY build/libs/resources /app/resources

VOLUME /cache

EXPOSE 8080

RUN apt update && DEBIAN_FRONTEND=noninteractive apt-get install -y tzdata

RUN export DEBIAN_FRONTEND=noninteractive
RUN export TZ=Etc/UTC

RUN apt install software-properties-common curl wget -y
RUN add-apt-repository ppa:deadsnakes/ppa

RUN wget https://download.oracle.com/java/21/latest/jdk-21_linux-x64_bin.deb
RUN wget https://bootstrap.pypa.io/get-pip.py

RUN apt-get -qqy install ./jdk-21_linux-x64_bin.deb
RUN #update-alternatives --install /usr/bin/java java /usr/lib/jvm/jdk-21/bin/java 1919

RUN apt-get update -y
RUN apt install python3.11 -y
RUN #apt install python3-pip -y
RUN python3.11 --version
RUN python3.11 get-pip.py

RUN python3.11 -m pip install -U pip
RUN python3.11 -m pip install --no-cache-dir -r /app/resources/python/requirements.txt

ENV PYTHON_BIN=python3.11
ENV PYTHON_DAEMON_WORKERS=6
ENV TENSOR_PYTHON_HOST=localhost
#ENV TENSOR_PYTHON_PORT=1099 # without = random
ENV TENSOR_PYTHON_PASS_PIP_INSTALLATION=true
ENV TENSOR_PYTHON_PASS_MODEL_PREFETCHING=false
ENV TENSOR_PYTHON_PATH_FETCHER_FILE=/app/resources/python/fetch_models.py
ENV TENSOR_PYTHON_PATH_REQ_FILE=/app/resources/python/requirements.txt
ENV TENSOR_PYTHON_PATH_MAIN_FILE=/app/resources/python/main.py
ENV HUGGINGFACE_HUB_CACHE=/cache/HUGGINGFACE_HUB_CACHE
ENV TRANSFORMERS_CACHE=/cache/TRANSFORMERS_CACHE
ENV HF_HOME=/cache/HF_HOME
ENV RUN_KTOR_FORCE=false

CMD ["java", "-jar", "/app/main.jar"]
