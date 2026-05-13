FROM docker:29-dind

RUN apk add --no-cache \
    openssh \
    bash \
    curl \
    git \
    docker-cli-compose

RUN ssh-keygen -A

RUN adduser -D -s /bin/bash deployer \
    && echo "deployer:deployer" | chpasswd

RUN mkdir -p /home/deployer/.ssh \
    && chown -R deployer:deployer /home/deployer/.ssh \
    && chmod 700 /home/deployer/.ssh

RUN mkdir -p /opt/cloud-task-manager \
    && chown -R deployer:deployer /opt/cloud-task-manager

EXPOSE 22 2375

CMD ["sh", "-c", "dockerd-entrypoint.sh & /usr/sbin/sshd -D -e"]