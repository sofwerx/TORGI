all:
	docker-compose build torgi
	docker-compose up -d --force-recreate torgi
	docker exec torgi find /torgi/ -name '*.apk' | while read line ; do \
		docker cp torgi:$$line . ; \
	done
	docker-compose down
