// Read-only validation of public Docker image manifests. No daemon, credentials or downloads required.
const images=['pgvector/pgvector:pg17','library/postgres:17-alpine','prom/prometheus:v3.7.2','jaegertracing/all-in-one:1.76.0','grafana/loki:3.5.7','otel/opentelemetry-collector-contrib:0.137.0','library/maven:3.9.11-eclipse-temurin-21','library/eclipse-temurin:21-jre-alpine','library/node:24-alpine'];
for(const image of images){const split=image.lastIndexOf(':'),repo=image.slice(0,split),tag=image.slice(split+1);
 const auth=await fetch(`https://auth.docker.io/token?service=registry.docker.io&scope=repository:${repo}:pull`);if(!auth.ok)throw new Error(`Token lookup failed: ${repo} ${auth.status}`);
 const {token}=await auth.json();const res=await fetch(`https://registry-1.docker.io/v2/${repo}/manifests/${tag}`,{method:'HEAD',headers:{Authorization:`Bearer ${token}`,Accept:'application/vnd.oci.image.index.v1+json, application/vnd.docker.distribution.manifest.list.v2+json, application/vnd.docker.distribution.manifest.v2+json'}});
 console.log(`${res.status} ${image}`);if(!res.ok)process.exitCode=1;
}
