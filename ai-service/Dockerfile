FROM python:3.13-slim AS build
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir --prefix=/install -r requirements.txt

FROM python:3.13-slim
RUN useradd --system --uid 10001 reliefgrid
WORKDIR /app

# Copy installed dependencies into system site-packages directly
COPY --from=build /install /usr/local
COPY app app

ENV PYTHONPATH=/app

# Bake the model into the image at build time
RUN python -m app.ml.train

USER reliefgrid
EXPOSE 8000
HEALTHCHECK --interval=10s --timeout=5s --start-period=15s --retries=10 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health')" || exit 1
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
