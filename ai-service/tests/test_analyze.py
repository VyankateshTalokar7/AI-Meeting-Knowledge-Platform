import pytest
from unittest.mock import AsyncMock, patch
from fastapi.testclient import TestClient
from app.main import app
from app.config import get_settings, Settings
from app.schemas.analysis import MeetingAnalysisSchema, ActionItemSchema
from app.services.llm_service import get_llm_service, LLMService, LLMError, OpenRouterLLMService

client = TestClient(app)


def test_empty_transcript_returns_empty_knowledge():
    response = client.post("/analyze", json={"text": "   "})
    assert response.status_code == 200
    data = response.json()
    assert data["summary"] == ""
    assert data["key_topics"] == []
    assert data["decisions"] == []
    assert data["action_items"] == []


def test_transcript_too_long_returns_400():
    def get_test_settings():
        return Settings(max_analysis_transcript_chars=50, openrouter_api_key="test_key")

    app.dependency_overrides[get_settings] = get_test_settings
    try:
        response = client.post("/analyze", json={"text": "A" * 100})
        assert response.status_code == 400
        assert "exceeds maximum allowed limit" in response.json()["detail"]
    finally:
        app.dependency_overrides.clear()


def test_missing_api_key_returns_500_without_exposing_secrets():
    def get_test_settings():
        return Settings(openrouter_api_key="")

    app.dependency_overrides[get_settings] = get_test_settings
    try:
        response = client.post("/analyze", json={"text": "Valid transcript text."})
        assert response.status_code == 500
        assert "LLM meeting analysis failed." in response.json()["detail"]
        assert "api_key" not in response.json()["detail"].lower()
    finally:
        app.dependency_overrides.clear()


def test_successful_analysis_with_mocked_llm_service():
    mock_llm_service = AsyncMock(spec=LLMService)
    mock_llm_service.analyze_meeting.return_value = MeetingAnalysisSchema(
        summary="The team discussed project milestones.",
        key_topics=["Speech-to-Text", "LLM Integration"],
        decisions=["Approved OpenRouter integration"],
        action_items=[
            ActionItemSchema(
                task="Complete LLM service layer",
                assignee="John",
                due_date="2026-09-20"
            ),
            ActionItemSchema(
                task="Update documentation",
                assignee=None,
                due_date=None
            )
        ]
    )

    app.dependency_overrides[get_llm_service] = lambda: mock_llm_service

    try:
        response = client.post("/analyze", json={"text": "Team meeting discussion transcript..."})
        assert response.status_code == 200
        data = response.json()

        assert data["summary"] == "The team discussed project milestones."
        assert data["key_topics"] == ["Speech-to-Text", "LLM Integration"]
        assert data["decisions"] == ["Approved OpenRouter integration"]
        assert len(data["action_items"]) == 2

        item1 = data["action_items"][0]
        assert item1["task"] == "Complete LLM service layer"
        assert item1["assignee"] == "John"
        assert item1["due_date"] == "2026-09-20"

        item2 = data["action_items"][1]
        assert item2["task"] == "Update documentation"
        assert item2["assignee"] is None
        assert item2["due_date"] is None
    finally:
        app.dependency_overrides.clear()


def test_invalid_calendar_date_becomes_none():
    invalid_item = ActionItemSchema(task="Invalid date test", assignee="Bob", due_date="2026-02-31")
    assert invalid_item.due_date is None


def test_llm_service_error_handling():
    mock_llm_service = AsyncMock(spec=LLMService)
    mock_llm_service.analyze_meeting.side_effect = LLMError("OpenRouter service failure")

    app.dependency_overrides[get_llm_service] = lambda: mock_llm_service

    try:
        response = client.post("/analyze", json={"text": "Transcript content"})
        assert response.status_code == 500
        assert response.json()["detail"] == "LLM meeting analysis failed."
    finally:
        app.dependency_overrides.clear()


@pytest.mark.anyio
async def test_openrouter_service_http_mock():
    settings = Settings(
        openrouter_api_key="sk-test-secret-key-12345",
        openrouter_base_url="https://openrouter.ai/api/v1",
        openrouter_model="openai/gpt-4o-mini"
    )
    service = OpenRouterLLMService(settings)

    mock_llm_json = {
        "choices": [
            {
                "message": {
                    "content": '{"summary": "Test summary", "key_topics": ["T1"], "decisions": [], "action_items": []}'
                }
            }
        ]
    }

    with patch("httpx.AsyncClient.post") as mock_post:
        mock_response = AsyncMock()
        mock_response.status_code = 200
        mock_response.raise_for_status = lambda: None
        mock_response.json = lambda: mock_llm_json
        mock_post.return_value = mock_response

        result = await service.analyze_meeting("Test transcript")
        assert result.summary == "Test summary"
        assert result.key_topics == ["T1"]
        assert result.decisions == []
        assert result.action_items == []
