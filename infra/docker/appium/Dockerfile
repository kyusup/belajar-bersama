FROM appium/appium:latest

USER root

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/* \
  && appium driver install chromium

# Keep the image default user (androidusr) so APPIUM_HOME resolves correctly.
