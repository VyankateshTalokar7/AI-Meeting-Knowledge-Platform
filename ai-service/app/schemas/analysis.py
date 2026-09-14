from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field, field_validator


class ActionItemSchema(BaseModel):
    task: str = Field(description="Action item task description")
    assignee: Optional[str] = Field(default=None, description="Person assigned to task, or None/null if unassigned or unknown")
    due_date: Optional[str] = Field(default=None, description="Due date in YYYY-MM-DD format, or None/null if not explicitly specified")

    @field_validator("due_date", mode="before")
    @classmethod
    def validate_due_date(cls, value: Optional[str]) -> Optional[str]:
        if not value or not isinstance(value, str):
            return None
        value = value.strip()
        if not value or value.lower() in ("null", "none", "unknown", "n/a"):
            return None
        try:
            parsed_date = datetime.strptime(value, "%Y-%m-%d")
            return parsed_date.strftime("%Y-%m-%d")
        except ValueError:
            return None

    @field_validator("assignee", mode="before")
    @classmethod
    def validate_assignee(cls, value: Optional[str]) -> Optional[str]:
        if not value or not isinstance(value, str):
            return None
        value = value.strip()
        if not value or value.lower() in ("null", "none", "unassigned", "unknown", "n/a"):
            return None
        return value


class MeetingAnalysisSchema(BaseModel):
    summary: str = Field(description="A concise executive summary based strictly on the transcript")
    key_topics: list[str] = Field(default_factory=list, description="Key topics discussed in the meeting")
    decisions: list[str] = Field(default_factory=list, description="Decisions made during the meeting")
    action_items: list[ActionItemSchema] = Field(default_factory=list, description="Action items assigned or agreed upon")


class AnalysisRequest(BaseModel):
    text: str = Field(description="Meeting transcript text to analyze")
