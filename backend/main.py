import json
import os
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from openai import OpenAI

app = FastAPI(title="JARVIS Cloud Brain", version="1.0")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
client = OpenAI(api_key=os.environ.get("OPENAI_API_KEY"))
MODEL = os.environ.get("OPENAI_MODEL", "gpt-4.1-mini")

class Command(BaseModel):
    text: str

@app.get("/")
def health():
    return {"status": "online", "assistant": "JARVIS"}

@app.post("/command")
def command(body: Command):
    if not os.environ.get("OPENAI_API_KEY"):
        raise HTTPException(503, "Server is missing OPENAI_API_KEY")
    instructions = """You are JARVIS, a concise, polite personal assistant. Convert the user's request into exactly one JSON object with keys: action, reply, query, phone, message, app. Allowed action values: answer, youtube, dial, sms, open_app. Use empty strings for unused fields. For YouTube, put the requested video/song/topic in query. For dial, put the explicitly supplied phone number in phone; if only a contact name is supplied, use action answer and ask the user to choose the contact in their phone. For sms, only use a phone number explicitly supplied by the user; otherwise ask them to choose a recipient in the messaging app. Put the message body in message. For open_app, app should be one of youtube, whatsapp, chrome, maps, settings if clearly requested. Never claim a message was sent or a call was completed. Android will only open a dialer or prefilled message screen, and the user must confirm. For everything else, action answer and provide a helpful reply. Do not execute or suggest hidden actions."""
    try:
        response = client.responses.create(
            model=MODEL,
            instructions=instructions,
            input=body.text,
            text={"format": {"type": "json_object"}},
        )
        data = json.loads(response.output_text)
        allowed = {"answer", "youtube", "dial", "sms", "open_app"}
        if data.get("action") not in allowed:
            raise ValueError("Invalid action")
        for key in ("reply", "query", "phone", "message", "app"):
            data[key] = str(data.get(key, ""))
        return data
    except Exception as exc:
        raise HTTPException(500, f"JARVIS could not process that request: {exc}")
