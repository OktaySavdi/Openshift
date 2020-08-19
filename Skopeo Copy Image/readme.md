## Skopeo Copy Image

```ruby
oc whoami -t
token1=`oc whoami -t`

skopeo copy --src-tls-verify=false --src-creds MyUsernameRepo1:$token1 --dest-tls-verify=false --dest-creds MyUsernameRepo2:$token2 docker://MyDockerRepository1/project_name/nginx:1.0 docker://MyDockerRepository2/project_name/nginx:1.0
```
